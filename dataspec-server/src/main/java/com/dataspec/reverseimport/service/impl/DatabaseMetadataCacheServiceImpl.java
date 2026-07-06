package com.dataspec.reverseimport.service.impl;

import com.dataspec.common.exception.BizException;
import com.dataspec.common.sanitize.SensitiveDataSanitizer;
import com.dataspec.reverseimport.entity.DatabaseMetadataCacheEntry;
import com.dataspec.reverseimport.model.DatabaseConnectionReq;
import com.dataspec.reverseimport.model.DatabaseMetadataCacheInfo;
import com.dataspec.reverseimport.model.DatabaseMetadataCacheMode;
import com.dataspec.reverseimport.model.DatabaseMetadataChangeSummary;
import com.dataspec.reverseimport.model.DatabaseMetadataColumnChange;
import com.dataspec.reverseimport.model.DatabaseMetadataTableChange;
import com.dataspec.reverseimport.model.DatabaseSchemaColumn;
import com.dataspec.reverseimport.model.DatabaseSchemaDump;
import com.dataspec.reverseimport.model.DatabaseSchemaIndex;
import com.dataspec.reverseimport.model.DatabaseSchemaSource;
import com.dataspec.reverseimport.model.DatabaseSchemaTable;
import com.dataspec.reverseimport.model.ReverseImportFieldChange;
import com.dataspec.reverseimport.repository.DatabaseMetadataCacheRepository;
import com.dataspec.reverseimport.service.DatabaseMetadataCacheService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

/**
 * 默认数据库 metadata cache 服务实现。缓存只保存 schema-only 快照，并对所有可见文本做脱敏处理。
 */
@Service
public class DatabaseMetadataCacheServiceImpl implements DatabaseMetadataCacheService {

    private static final int DEFAULT_TTL_HOURS = 24;
    private static final int MAX_CHANGE_EXAMPLES = 20;
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_INSTANT;

    private final DatabaseMetadataCacheRepository repository;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Autowired
    public DatabaseMetadataCacheServiceImpl(DatabaseMetadataCacheRepository repository, ObjectMapper objectMapper) {
        this(repository, objectMapper, Clock.systemUTC());
    }

    public DatabaseMetadataCacheServiceImpl(DatabaseMetadataCacheRepository repository, ObjectMapper objectMapper, Clock clock) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Override
    public DatabaseSchemaDump resolveDump(DatabaseConnectionReq req, Supplier<DatabaseSchemaDump> sourceLoader) {
        DatabaseMetadataCacheMode mode = cacheMode(req);
        if (mode == DatabaseMetadataCacheMode.BYPASS) {
            DatabaseSchemaDump dump = sanitizeDump(sourceLoader.get(), req);
            dump.setMetadataCache(bypassCacheInfo(dump));
            return dump;
        }

        String sourceScopeHash = sourceScopeHash(req);
        List<String> tableNames = normalizeTableNames(req.getTableNames());
        List<DatabaseMetadataCacheEntry> cachedEntries = findEntries(req, sourceScopeHash, tableNames);
        boolean allFresh = !tableNames.isEmpty()
                && cachedEntries.size() == tableNames.size()
                && cachedEntries.stream().allMatch(this::isFresh);

        if (mode == DatabaseMetadataCacheMode.AUTO && allFresh) {
            DatabaseSchemaDump dump = buildDumpFromCache(req, cachedEntries);
            dump.setMetadataCache(cacheInfo(cachedEntries, DatabaseMetadataCacheMode.AUTO, true, false, new DatabaseMetadataChangeSummary()));
            return dump;
        }

        DatabaseSchemaDump freshDump = loadFreshDump(req, sourceLoader, cachedEntries, mode);
        DatabaseMetadataChangeSummary summary = changeSummary(cachedEntries, freshDump);
        expireRemovedEntries(cachedEntries, freshDump, mode, summary);
        List<DatabaseMetadataCacheEntry> savedEntries = upsertDump(req, sourceScopeHash, freshDump, mode, summary);
        boolean stale = cachedEntries.stream().anyMatch(entry -> !isFresh(entry));
        freshDump.setMetadataCache(cacheInfo(savedEntries, mode, false, stale, summary));
        return freshDump;
    }

    @Override
    public DatabaseMetadataCacheInfo summarize(DatabaseConnectionReq req, List<String> tableNames) {
        DatabaseMetadataCacheMode mode = cacheMode(req);
        if (mode == DatabaseMetadataCacheMode.BYPASS) {
            DatabaseMetadataCacheInfo info = new DatabaseMetadataCacheInfo();
            info.setRefreshMode(DatabaseMetadataCacheMode.BYPASS.name());
            info.getNextActions().add("当前请求绕过 metadata cache。");
            return info;
        }
        String sourceScopeHash = sourceScopeHash(req);
        List<String> requestedTableNames = normalizeTableNames(tableNames);
        List<DatabaseMetadataCacheEntry> entries = findEntries(req, sourceScopeHash, requestedTableNames);
        Set<String> hitTableNames = new LinkedHashSet<>();
        for (DatabaseMetadataCacheEntry entry : entries) {
            hitTableNames.add(normalizeTableName(entry.getTableName()));
        }
        boolean allRequestedTablesHit = !requestedTableNames.isEmpty()
                && hitTableNames.size() == requestedTableNames.size()
                && hitTableNames.containsAll(requestedTableNames);
        boolean cacheHit = allRequestedTablesHit && entries.stream().allMatch(this::isFresh);
        boolean stale = entries.stream().anyMatch(entry -> !isFresh(entry));
        return cacheInfo(entries, mode, cacheHit, stale, new DatabaseMetadataChangeSummary());
    }

    private DatabaseSchemaDump loadFreshDump(DatabaseConnectionReq req,
                                             Supplier<DatabaseSchemaDump> sourceLoader,
                                             List<DatabaseMetadataCacheEntry> cachedEntries,
                                             DatabaseMetadataCacheMode mode) {
        try {
            return sanitizeDump(sourceLoader.get(), req);
        } catch (BizException e) {
            if (mode == DatabaseMetadataCacheMode.REFRESH && !cachedEntries.isEmpty() && isSelectedTablesMissing(e)) {
                return emptyDump(req);
            }
            throw e;
        }
    }

    private List<DatabaseMetadataCacheEntry> findEntries(DatabaseConnectionReq req,
                                                         String sourceScopeHash,
                                                         List<String> tableNames) {
        List<DatabaseMetadataCacheEntry> entries = new ArrayList<>();
        String schemaName = normalizeSchemaName(req);
        for (String tableName : tableNames) {
            repository.findActive(req.getProjectId(), sourceScopeHash, schemaName, tableName)
                    .ifPresent(entries::add);
        }
        return entries;
    }

    private void expireRemovedEntries(List<DatabaseMetadataCacheEntry> cachedEntries,
                                      DatabaseSchemaDump freshDump,
                                      DatabaseMetadataCacheMode mode,
                                      DatabaseMetadataChangeSummary summary) {
        if (cachedEntries.isEmpty()) {
            return;
        }
        Set<String> freshTableNames = new LinkedHashSet<>();
        for (DatabaseSchemaTable table : freshDump.getTables()) {
            freshTableNames.add(normalizeTableName(table.getTableName()));
        }
        String changeSummaryJson = writeJson(summary);
        LocalDateTime now = now();
        for (DatabaseMetadataCacheEntry entry : cachedEntries) {
            if (freshTableNames.contains(normalizeTableName(entry.getTableName()))) {
                continue;
            }
            // REFRESH 已确认该表不存在时只把旧缓存置为过期，保留唯一键以便表重新出现后直接更新。
            entry.setLastSeenAt(now);
            entry.setExpiresAt(now);
            entry.setRefreshMode(mode.name());
            entry.setChangeSummaryJson(changeSummaryJson);
            repository.expire(entry);
        }
    }

    private List<DatabaseMetadataCacheEntry> upsertDump(DatabaseConnectionReq req,
                                                        String sourceScopeHash,
                                                        DatabaseSchemaDump dump,
                                                        DatabaseMetadataCacheMode mode,
                                                        DatabaseMetadataChangeSummary summary) {
        List<DatabaseMetadataCacheEntry> entries = new ArrayList<>();
        String changeSummaryJson = writeJson(summary);
        for (DatabaseSchemaTable table : dump.getTables()) {
            DatabaseMetadataCacheEntry entry = buildEntry(req, sourceScopeHash, dump, table, mode, changeSummaryJson);
            repository.upsert(entry);
            entries.add(entry);
        }
        return entries;
    }

    private DatabaseMetadataCacheEntry buildEntry(DatabaseConnectionReq req,
                                                  String sourceScopeHash,
                                                  DatabaseSchemaDump dump,
                                                  DatabaseSchemaTable table,
                                                  DatabaseMetadataCacheMode mode,
                                                  String changeSummaryJson) {
        LocalDateTime now = now();
        DatabaseMetadataCacheEntry entry = new DatabaseMetadataCacheEntry();
        entry.setProjectId(req.getProjectId());
        entry.setPresetId(req.getPresetId());
        entry.setSourceScopeHash(sourceScopeHash);
        entry.setDatabaseType(safeText(dump.getDatabaseType(), req));
        entry.setDatabaseName(safeText(dump.getDatabaseName(), req));
        entry.setSchemaName(normalizeSchemaName(req));
        entry.setTableName(normalizeTableName(table.getTableName()));
        entry.setTableFingerprint(tableFingerprint(table));
        entry.setMetadataJson(writeJson(normalizedTable(table)));
        DatabaseSchemaSource source = dump.getSource();
        entry.setSourceProductName(safeText(source == null ? null : source.getDatabaseProductName(), req));
        entry.setSourceProductVersion(safeText(source == null ? null : source.getDatabaseProductVersion(), req));
        entry.setFirstSeenAt(now);
        entry.setLastSeenAt(now);
        entry.setExpiresAt(now.plusHours(DEFAULT_TTL_HOURS));
        entry.setRefreshMode(mode.name());
        entry.setChangeSummaryJson(changeSummaryJson);
        entry.setIsDeleted(false);
        return entry;
    }

    private DatabaseSchemaDump buildDumpFromCache(DatabaseConnectionReq req, List<DatabaseMetadataCacheEntry> entries) {
        List<DatabaseMetadataCacheEntry> sorted = entries.stream()
                .sorted(Comparator.comparing(DatabaseMetadataCacheEntry::getTableName, Comparator.nullsLast(String::compareToIgnoreCase)))
                .toList();
        DatabaseSchemaDump dump = new DatabaseSchemaDump();
        dump.setProjectId(req.getProjectId());
        dump.setDatabaseType(safeText(databaseType(req).toUpperCase(Locale.ROOT), req));
        dump.setDatabaseName(safeText(req.getDatabaseName(), req));
        dump.setSchemaName(safeText(normalizeSchemaName(req), req));
        dump.setGeneratedAt(ISO_FORMATTER.format(clock.instant()));

        DatabaseSchemaSource source = new DatabaseSchemaSource();
        source.setSourceType("metadata-cache");
        if (!sorted.isEmpty()) {
            DatabaseMetadataCacheEntry first = sorted.get(0);
            source.setDatabaseProductName(first.getSourceProductName());
            source.setDatabaseProductVersion(first.getSourceProductVersion());
        }
        source.setSchemaName(safeText(normalizeSchemaName(req), req));
        source.setSelectedTableNames(sorted.stream().map(DatabaseMetadataCacheEntry::getTableName).toList());
        source.setTableCount(sorted.size());
        dump.setSource(source);

        for (DatabaseMetadataCacheEntry entry : sorted) {
            dump.getTables().add(readTable(entry.getMetadataJson()));
        }
        return dump;
    }

    private DatabaseMetadataCacheInfo cacheInfo(DatabaseSchemaDump dump,
                                                DatabaseMetadataCacheMode mode,
                                                boolean cacheHit,
                                                boolean stale,
                                                DatabaseMetadataChangeSummary summary) {
        List<DatabaseMetadataCacheEntry> entries = dump.getTables().stream()
                .map(table -> {
                    DatabaseMetadataCacheEntry entry = new DatabaseMetadataCacheEntry();
                    entry.setTableFingerprint(tableFingerprint(table));
                    entry.setLastSeenAt(now());
                    entry.setExpiresAt(now().plusHours(DEFAULT_TTL_HOURS));
                    DatabaseSchemaSource source = dump.getSource();
                    entry.setSourceProductName(source == null ? null : source.getDatabaseProductName());
                    entry.setSourceProductVersion(source == null ? null : source.getDatabaseProductVersion());
                    return entry;
                })
                .toList();
        return cacheInfo(entries, mode, cacheHit, stale, summary);
    }

    private DatabaseMetadataCacheInfo bypassCacheInfo(DatabaseSchemaDump dump) {
        DatabaseMetadataCacheInfo info = new DatabaseMetadataCacheInfo();
        info.setRefreshMode(DatabaseMetadataCacheMode.BYPASS.name());
        info.setCacheHit(false);
        info.setStale(false);
        info.setMetadataFingerprint(aggregateFingerprint(dump.getTables().stream()
                .map(this::tableFingerprint)
                .toList()));
        info.setLastSeenAt(format(now()));
        DatabaseSchemaSource source = dump.getSource();
        if (source != null) {
            info.setSourceDatabaseVersion(
                    String.join(" ", List.of(nullToEmpty(source.getDatabaseProductName()), nullToEmpty(source.getDatabaseProductVersion()))).trim());
        }
        info.setChangeSummary(new DatabaseMetadataChangeSummary());
        info.getNextActions().add("本次绕过 metadata cache，不会读取或更新缓存。");
        return info;
    }

    private DatabaseMetadataCacheInfo cacheInfo(List<DatabaseMetadataCacheEntry> entries,
                                                DatabaseMetadataCacheMode mode,
                                                boolean cacheHit,
                                                boolean stale,
                                                DatabaseMetadataChangeSummary summary) {
        DatabaseMetadataCacheInfo info = new DatabaseMetadataCacheInfo();
        info.setRefreshMode(mode.name());
        info.setCacheHit(cacheHit);
        info.setStale(stale);
        info.setChangeSummary(summary == null ? new DatabaseMetadataChangeSummary() : summary);
        if (!entries.isEmpty()) {
            info.setMetadataFingerprint(aggregateFingerprint(entries.stream()
                    .map(DatabaseMetadataCacheEntry::getTableFingerprint)
                    .filter(Objects::nonNull)
                    .toList()));
            entries.stream()
                    .map(DatabaseMetadataCacheEntry::getLastSeenAt)
                    .filter(Objects::nonNull)
                    .max(LocalDateTime::compareTo)
                    .ifPresent(value -> info.setLastSeenAt(format(value)));
            entries.stream()
                    .map(DatabaseMetadataCacheEntry::getExpiresAt)
                    .filter(Objects::nonNull)
                    .min(LocalDateTime::compareTo)
                    .ifPresent(value -> info.setExpiresAt(format(value)));
            entries.stream()
                    .filter(entry -> entry.getSourceProductName() != null || entry.getSourceProductVersion() != null)
                    .findFirst()
                    .ifPresent(entry -> info.setSourceDatabaseVersion(
                            String.join(" ", List.of(nullToEmpty(entry.getSourceProductName()), nullToEmpty(entry.getSourceProductVersion()))).trim()));
        }
        if (cacheHit) {
            info.getNextActions().add("metadata cache 命中，可直接复用 fingerprint 判断是否重跑下游分析。");
        } else if (mode == DatabaseMetadataCacheMode.REFRESH) {
            info.getNextActions().add("metadata 已强制刷新，请根据 changeSummary 判断是否重跑反向导入或覆盖率。");
        } else if (mode == DatabaseMetadataCacheMode.BYPASS) {
            info.getNextActions().add("本次绕过 metadata cache，不会更新缓存。");
        } else {
            info.getNextActions().add("metadata cache 已更新，可在重复扫描时复用。");
        }
        return info;
    }

    private DatabaseMetadataChangeSummary changeSummary(List<DatabaseMetadataCacheEntry> cachedEntries, DatabaseSchemaDump freshDump) {
        Map<String, DatabaseMetadataCacheEntry> cachedByTable = new LinkedHashMap<>();
        for (DatabaseMetadataCacheEntry entry : cachedEntries) {
            cachedByTable.put(normalizeTableName(entry.getTableName()), entry);
        }
        DatabaseMetadataChangeSummary summary = new DatabaseMetadataChangeSummary();
        for (DatabaseSchemaTable newTable : freshDump.getTables()) {
            String tableName = normalizeTableName(newTable.getTableName());
            DatabaseMetadataCacheEntry oldEntry = cachedByTable.remove(tableName);
            DatabaseMetadataTableChange tableChange = oldEntry == null
                    ? addedTableChange(newTable)
                    : compareTable(readTable(oldEntry.getMetadataJson()), newTable, oldEntry.getTableFingerprint(), tableFingerprint(newTable));
            addTableChange(summary, tableChange);
        }
        for (DatabaseMetadataCacheEntry removed : cachedByTable.values()) {
            addTableChange(summary, removedTableChange(readTable(removed.getMetadataJson()), removed.getTableFingerprint()));
        }
        summary.setChanged(summary.getAddedTableCount() > 0
                || summary.getRemovedTableCount() > 0
                || summary.getChangedTableCount() > 0
                || summary.getAddedColumnCount() > 0
                || summary.getRemovedColumnCount() > 0
                || summary.getChangedColumnCount() > 0);
        return summary;
    }

    private DatabaseMetadataTableChange addedTableChange(DatabaseSchemaTable table) {
        DatabaseMetadataTableChange change = baseTableChange(table);
        change.setChangeType("ADDED");
        change.setNewFingerprint(tableFingerprint(table));
        change.setAddedColumns(table.getColumns().stream().map(DatabaseSchemaColumn::getColumnName).toList());
        return change;
    }

    private DatabaseMetadataTableChange removedTableChange(DatabaseSchemaTable table, String oldFingerprint) {
        DatabaseMetadataTableChange change = baseTableChange(table);
        change.setChangeType("REMOVED");
        change.setOldFingerprint(oldFingerprint);
        change.setRemovedColumns(table.getColumns().stream().map(DatabaseSchemaColumn::getColumnName).toList());
        return change;
    }

    private DatabaseMetadataTableChange compareTable(DatabaseSchemaTable oldTable,
                                                     DatabaseSchemaTable newTable,
                                                     String oldFingerprint,
                                                     String newFingerprint) {
        DatabaseMetadataTableChange change = baseTableChange(newTable);
        change.setChangeType("UNCHANGED");
        change.setOldFingerprint(oldFingerprint);
        change.setNewFingerprint(newFingerprint);
        Map<String, DatabaseSchemaColumn> oldColumns = columnsByName(oldTable);
        Map<String, DatabaseSchemaColumn> newColumns = columnsByName(newTable);
        Map<String, String> oldIndexMembership = indexMembershipByColumn(oldTable);
        Map<String, String> newIndexMembership = indexMembershipByColumn(newTable);
        for (String name : newColumns.keySet()) {
            if (!oldColumns.containsKey(name)) {
                change.getAddedColumns().add(newColumns.get(name).getColumnName());
            }
        }
        for (String name : oldColumns.keySet()) {
            if (!newColumns.containsKey(name)) {
                change.getRemovedColumns().add(oldColumns.get(name).getColumnName());
            }
        }
        for (String name : newColumns.keySet()) {
            if (oldColumns.containsKey(name)) {
                DatabaseMetadataColumnChange columnChange = compareColumn(
                        oldColumns.get(name),
                        newColumns.get(name),
                        oldIndexMembership.get(name),
                        newIndexMembership.get(name));
                if (!columnChange.getChanges().isEmpty()) {
                    change.getChangedColumns().add(columnChange);
                }
            }
        }
        if (!change.getAddedColumns().isEmpty() || !change.getRemovedColumns().isEmpty() || !change.getChangedColumns().isEmpty()
                || !Objects.equals(normalizedIndexes(oldTable.getIndexes()), normalizedIndexes(newTable.getIndexes()))) {
            change.setChangeType("CHANGED");
        }
        return change;
    }

    private DatabaseMetadataColumnChange compareColumn(DatabaseSchemaColumn oldColumn,
                                                       DatabaseSchemaColumn newColumn,
                                                       String oldIndexMembership,
                                                       String newIndexMembership) {
        DatabaseMetadataColumnChange change = new DatabaseMetadataColumnChange();
        change.setColumnName(newColumn.getColumnName());
        addChange(change, "dataType", oldColumn.getDataType(), newColumn.getDataType());
        addChange(change, "nullable", String.valueOf(oldColumn.getNullable()), String.valueOf(newColumn.getNullable()));
        addChange(change, "defaultValue", oldColumn.getDefaultValue(), newColumn.getDefaultValue());
        addChange(change, "comment", oldColumn.getComment(), newColumn.getComment());
        addChange(change, "ordinalPosition", String.valueOf(oldColumn.getOrdinalPosition()), String.valueOf(newColumn.getOrdinalPosition()));
        addChange(change, "indexMembership", oldIndexMembership, newIndexMembership);
        return change;
    }

    private void addChange(DatabaseMetadataColumnChange columnChange, String property, String oldValue, String newValue) {
        if (!Objects.equals(nullToEmpty(oldValue), nullToEmpty(newValue))) {
            columnChange.getChanges().add(new ReverseImportFieldChange(property, oldValue, newValue));
        }
    }

    private void addTableChange(DatabaseMetadataChangeSummary summary, DatabaseMetadataTableChange tableChange) {
        if ("ADDED".equals(tableChange.getChangeType())) {
            summary.setAddedTableCount(summary.getAddedTableCount() + 1);
            summary.setAddedColumnCount(summary.getAddedColumnCount() + tableChange.getAddedColumns().size());
        } else if ("REMOVED".equals(tableChange.getChangeType())) {
            summary.setRemovedTableCount(summary.getRemovedTableCount() + 1);
            summary.setRemovedColumnCount(summary.getRemovedColumnCount() + tableChange.getRemovedColumns().size());
        } else if ("CHANGED".equals(tableChange.getChangeType())) {
            summary.setChangedTableCount(summary.getChangedTableCount() + 1);
            summary.setAddedColumnCount(summary.getAddedColumnCount() + tableChange.getAddedColumns().size());
            summary.setRemovedColumnCount(summary.getRemovedColumnCount() + tableChange.getRemovedColumns().size());
            summary.setChangedColumnCount(summary.getChangedColumnCount() + tableChange.getChangedColumns().size());
        }
        if (!"UNCHANGED".equals(tableChange.getChangeType()) && summary.getTables().size() < MAX_CHANGE_EXAMPLES) {
            summary.getTables().add(tableChange);
        }
    }

    private DatabaseMetadataTableChange baseTableChange(DatabaseSchemaTable table) {
        DatabaseMetadataTableChange change = new DatabaseMetadataTableChange();
        change.setSchemaName(table.getSchemaName());
        change.setTableName(table.getTableName());
        return change;
    }

    private Map<String, DatabaseSchemaColumn> columnsByName(DatabaseSchemaTable table) {
        Map<String, DatabaseSchemaColumn> values = new LinkedHashMap<>();
        for (DatabaseSchemaColumn column : normalizedColumns(table.getColumns())) {
            values.put(normalizeTableName(column.getColumnName()), column);
        }
        return values;
    }

    private DatabaseSchemaDump emptyDump(DatabaseConnectionReq req) {
        DatabaseSchemaDump dump = new DatabaseSchemaDump();
        dump.setProjectId(req.getProjectId());
        dump.setDatabaseType(safeText(databaseType(req).toUpperCase(Locale.ROOT), req));
        dump.setDatabaseName(safeText(req.getDatabaseName(), req));
        dump.setSchemaName(safeText(normalizeSchemaName(req), req));
        dump.setGeneratedAt(ISO_FORMATTER.format(clock.instant()));
        DatabaseSchemaSource source = new DatabaseSchemaSource();
        source.setSourceType("jdbc-metadata");
        source.setSchemaName(safeText(normalizeSchemaName(req), req));
        source.setSelectedTableNames(req.getTableNames().stream().map(value -> safeText(value, req)).toList());
        source.setTableCount(0);
        dump.setSource(source);
        return dump;
    }

    private boolean isSelectedTablesMissing(BizException e) {
        return e.getMessage() != null && e.getMessage().contains("未找到所选表");
    }

    private Map<String, String> indexMembershipByColumn(DatabaseSchemaTable table) {
        Map<String, List<String>> grouped = new LinkedHashMap<>();
        for (Map<String, Object> index : normalizedIndexes(table.getIndexes())) {
            String columnName = normalizeTableName(String.valueOf(index.get("columnName")));
            if (columnName.isBlank()) {
                continue;
            }
            grouped.computeIfAbsent(columnName, ignored -> new ArrayList<>()).add(String.join(":",
                    String.valueOf(index.get("indexName")),
                    String.valueOf(index.get("nonUnique")),
                    String.valueOf(index.get("ordinalPosition"))));
        }
        Map<String, String> result = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> entry : grouped.entrySet()) {
            result.put(entry.getKey(), String.join("|", entry.getValue()));
        }
        return result;
    }

    private DatabaseSchemaDump sanitizeDump(DatabaseSchemaDump dump, DatabaseConnectionReq req) {
        dump.setDatabaseType(safeText(dump.getDatabaseType(), req));
        dump.setDatabaseName(safeText(dump.getDatabaseName(), req));
        dump.setSchemaName(safeText(dump.getSchemaName(), req));
        DatabaseSchemaSource source = dump.getSource();
        if (source != null) {
            source.setDatabaseProductName(safeText(source.getDatabaseProductName(), req));
            source.setDatabaseProductVersion(safeText(source.getDatabaseProductVersion(), req));
            source.setCatalogName(safeText(source.getCatalogName(), req));
            source.setSchemaName(safeText(source.getSchemaName(), req));
            source.setSelectedTableNames(source.getSelectedTableNames().stream().map(value -> safeText(value, req)).toList());
        }
        for (DatabaseSchemaTable table : dump.getTables()) {
            table.setSchemaName(safeText(table.getSchemaName(), req));
            table.setTableName(safeText(table.getTableName(), req));
            table.setTableType(safeText(table.getTableType(), req));
            table.setComment(safeText(table.getComment(), req));
            for (DatabaseSchemaColumn column : table.getColumns()) {
                column.setColumnName(safeText(column.getColumnName(), req));
                column.setDataType(safeText(column.getDataType(), req));
                column.setDefaultValue(safeText(column.getDefaultValue(), req));
                column.setComment(safeText(column.getComment(), req));
            }
            for (DatabaseSchemaIndex index : table.getIndexes()) {
                index.setSchemaName(safeText(index.getSchemaName(), req));
                index.setTableName(safeText(index.getTableName(), req));
                index.setIndexName(safeText(index.getIndexName(), req));
                index.setColumnName(safeText(index.getColumnName(), req));
            }
            table.setWarnings(table.getWarnings().stream().map(value -> safeText(value, req)).toList());
        }
        return dump;
    }

    private Object normalizedTable(DatabaseSchemaTable table) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("schemaName", nullToEmpty(table.getSchemaName()));
        value.put("tableName", nullToEmpty(table.getTableName()));
        value.put("tableType", nullToEmpty(table.getTableType()));
        value.put("comment", nullToEmpty(table.getComment()));
        value.put("columns", normalizedColumns(table.getColumns()).stream().map(this::normalizedColumn).toList());
        value.put("indexes", normalizedIndexes(table.getIndexes()));
        value.put("warnings", new ArrayList<>(new LinkedHashSet<>(table.getWarnings())));
        return value;
    }

    private Object normalizedColumn(DatabaseSchemaColumn column) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("columnName", nullToEmpty(column.getColumnName()));
        value.put("dataType", nullToEmpty(column.getDataType()));
        value.put("nullable", Boolean.TRUE.equals(column.getNullable()));
        value.put("defaultValue", nullToEmpty(column.getDefaultValue()));
        value.put("comment", nullToEmpty(column.getComment()));
        value.put("ordinalPosition", column.getOrdinalPosition() == null ? 0 : column.getOrdinalPosition());
        return value;
    }

    private List<DatabaseSchemaColumn> normalizedColumns(List<DatabaseSchemaColumn> columns) {
        if (columns == null) {
            return List.of();
        }
        return columns.stream()
                .sorted(Comparator
                        .comparing((DatabaseSchemaColumn column) -> column.getOrdinalPosition() == null ? Integer.MAX_VALUE : column.getOrdinalPosition())
                        .thenComparing(column -> normalizeTableName(column.getColumnName())))
                .toList();
    }

    private List<Map<String, Object>> normalizedIndexes(List<DatabaseSchemaIndex> indexes) {
        if (indexes == null) {
            return List.of();
        }
        return indexes.stream()
                .sorted(Comparator
                        .comparing((DatabaseSchemaIndex index) -> nullToEmpty(index.getIndexName()).toLowerCase(Locale.ROOT))
                        .thenComparing(index -> index.getOrdinalPosition() == null ? Integer.MAX_VALUE : index.getOrdinalPosition())
                        .thenComparing(index -> normalizeTableName(index.getColumnName())))
                .map(index -> {
                    Map<String, Object> value = new LinkedHashMap<>();
                    value.put("schemaName", nullToEmpty(index.getSchemaName()));
                    value.put("tableName", nullToEmpty(index.getTableName()));
                    value.put("indexName", nullToEmpty(index.getIndexName()));
                    value.put("columnName", nullToEmpty(index.getColumnName()));
                    value.put("nonUnique", Boolean.TRUE.equals(index.getNonUnique()));
                    value.put("ordinalPosition", index.getOrdinalPosition() == null ? 0 : index.getOrdinalPosition());
                    return value;
                })
                .toList();
    }

    private String tableFingerprint(DatabaseSchemaTable table) {
        return sha256(writeBytes(normalizedTable(table)));
    }

    private String aggregateFingerprint(List<String> fingerprints) {
        return sha256(String.join("\n", fingerprints.stream().sorted().toList()).getBytes(StandardCharsets.UTF_8));
    }

    private String sourceScopeHash(DatabaseConnectionReq req) {
        String presetPart = req.getPresetId() == null ? "-" : "preset:" + req.getPresetId();
        String text = String.join("\u001f",
                String.valueOf(req.getProjectId()),
                presetPart,
                databaseType(req),
                nullToEmpty(req.getHost()).trim().toLowerCase(Locale.ROOT),
                String.valueOf(defaultPort(req)),
                nullToEmpty(req.getDatabaseName()).trim().toLowerCase(Locale.ROOT),
                normalizeSchemaName(req),
                nullToEmpty(req.getUsername()).trim().toLowerCase(Locale.ROOT));
        return sha256(text.getBytes(StandardCharsets.UTF_8));
    }

    private boolean isFresh(DatabaseMetadataCacheEntry entry) {
        return entry.getExpiresAt() != null && entry.getExpiresAt().isAfter(now());
    }

    private DatabaseMetadataCacheMode cacheMode(DatabaseConnectionReq req) {
        if (req == null || req.getMetadataCacheMode() == null || req.getMetadataCacheMode().isBlank()) {
            return DatabaseMetadataCacheMode.AUTO;
        }
        try {
            return DatabaseMetadataCacheMode.valueOf(req.getMetadataCacheMode().trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return DatabaseMetadataCacheMode.AUTO;
        }
    }

    private List<String> normalizeTableNames(List<String> tableNames) {
        if (tableNames == null || tableNames.isEmpty()) {
            return List.of();
        }
        Set<String> values = new LinkedHashSet<>();
        for (String tableName : tableNames) {
            if (tableName != null && !tableName.isBlank()) {
                values.add(normalizeTableName(tableName));
            }
        }
        return List.copyOf(values);
    }

    private String normalizeSchemaName(DatabaseConnectionReq req) {
        if ("mysql".equals(databaseType(req))) {
            return "";
        }
        return req.getSchemaName() == null || req.getSchemaName().isBlank()
                ? "public"
                : req.getSchemaName().trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeTableName(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private String databaseType(DatabaseConnectionReq req) {
        return req.getDatabaseType() == null ? "" : req.getDatabaseType().trim().toLowerCase(Locale.ROOT);
    }

    private int defaultPort(DatabaseConnectionReq req) {
        if (req.getPort() != null) {
            return req.getPort();
        }
        return "mysql".equals(databaseType(req)) ? 3306 : 5432;
    }

    private LocalDateTime now() {
        return LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
    }

    private String format(LocalDateTime value) {
        return ISO_FORMATTER.format(value.toInstant(ZoneOffset.UTC));
    }

    private String safeText(String value, DatabaseConnectionReq req) {
        return SensitiveDataSanitizer.redactText(value, 1000, req == null ? null : req.getPassword());
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private String writeJson(Object value) {
        return new String(writeBytes(value), StandardCharsets.UTF_8);
    }

    private byte[] writeBytes(Object value) {
        try {
            return objectMapper.writeValueAsBytes(value);
        } catch (Exception e) {
            throw new IllegalStateException("metadata cache JSON 序列化失败", e);
        }
    }

    private DatabaseSchemaTable readTable(String metadataJson) {
        try {
            return objectMapper.readValue(metadataJson, DatabaseSchemaTable.class);
        } catch (Exception e) {
            throw new IllegalStateException("metadata cache JSON 反序列化失败", e);
        }
    }

    private String sha256(byte[] bytes) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(bytes);
            StringBuilder result = new StringBuilder(digest.length * 2);
            for (byte item : digest) {
                result.append(String.format("%02x", item));
            }
            return result.toString();
        } catch (Exception e) {
            throw new IllegalStateException("metadata fingerprint 计算失败", e);
        }
    }
}
