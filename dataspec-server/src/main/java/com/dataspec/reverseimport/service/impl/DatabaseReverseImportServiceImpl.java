package com.dataspec.reverseimport.service.impl;

import com.dataspec.common.exception.BizException;
import com.dataspec.common.sanitize.SensitiveDataSanitizer;
import com.dataspec.coverage.model.FieldCoverageItem;
import com.dataspec.coverage.model.FieldCoverageReport;
import com.dataspec.coverage.model.FieldCoverageStatus;
import com.dataspec.coverage.model.FieldCoverageTable;
import com.dataspec.coverage.service.FieldCoverageService;
import com.dataspec.dialect.service.SqlDialectCompatibilityService;
import com.dataspec.reverseimport.model.DatabaseConnectionHealthDiagnostic;
import com.dataspec.reverseimport.model.DatabaseConnectionReq;
import com.dataspec.reverseimport.model.DatabaseConnectionResult;
import com.dataspec.reverseimport.model.DatabaseConnectionSecurityDiagnostic;
import com.dataspec.reverseimport.model.DatabaseDialectCapability;
import com.dataspec.reverseimport.model.DatabaseSchemaDump;
import com.dataspec.reverseimport.model.DatabaseSchemaDumpReq;
import com.dataspec.reverseimport.model.DatabaseMetadataBrowser;
import com.dataspec.reverseimport.model.DatabaseMetadataBrowserColumn;
import com.dataspec.reverseimport.model.DatabaseMetadataBrowserSummary;
import com.dataspec.reverseimport.model.DatabaseMetadataBrowserTable;
import com.dataspec.reverseimport.model.DatabaseSchemaColumn;
import com.dataspec.reverseimport.model.DatabaseSchemaIndex;
import com.dataspec.reverseimport.model.DatabaseSchemaSource;
import com.dataspec.reverseimport.model.DatabaseSchemaTable;
import com.dataspec.reverseimport.model.DatabaseTableInfo;
import com.dataspec.reverseimport.model.FieldCandidate;
import com.dataspec.reverseimport.model.ReverseImportCompareResult;
import com.dataspec.reverseimport.model.ReverseImportCompareSummary;
import com.dataspec.reverseimport.model.ReverseImportFieldDiff;
import com.dataspec.reverseimport.model.ReverseImportFieldStatus;
import com.dataspec.reverseimport.model.ReverseImportPreview;
import com.dataspec.reverseimport.model.ReverseImportSummary;
import com.dataspec.reverseimport.model.ReverseImportTableDiff;
import com.dataspec.reverseimport.service.DatabaseMetadataAdapter;
import com.dataspec.reverseimport.service.DatabaseReverseImportService;
import com.dataspec.reverseimport.service.ReverseImportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

/**
 * 基于 JDBC metadata 的数据库直连反向导入。
 */
@Service
public class DatabaseReverseImportServiceImpl implements DatabaseReverseImportService {

    private static final String TYPE_POSTGRESQL = "postgresql";
    private static final String TYPE_MYSQL = "mysql";
    private static final String RISK_SAFE = "SAFE";
    private static final String RISK_WARNING = "WARNING";
    private static final String RISK_DANGER = "DANGER";
    private static final String RISK_UNKNOWN = "UNKNOWN";
    private static final String STATUS_CONNECTED = "CONNECTED";
    private static final String STATUS_FAILED = "FAILED";
    private static final String FAILURE_AUTHENTICATION = "AUTHENTICATION";
    private static final String FAILURE_NETWORK = "NETWORK";
    private static final String FAILURE_SCHEMA_NOT_FOUND = "SCHEMA_NOT_FOUND";
    private static final String FAILURE_PERMISSION_DENIED = "PERMISSION_DENIED";
    private static final String FAILURE_UNSUPPORTED_DIALECT = "UNSUPPORTED_DIALECT";
    private static final String FAILURE_UNKNOWN = "UNKNOWN";

    private final ReverseImportService reverseImportService;
    private final FieldCoverageService fieldCoverageService;
    private final ConnectionProvider connectionProvider;
    private final DatabaseMetadataAdapter metadataAdapter;
    private final SqlDialectCompatibilityService dialectCompatibilityService = new SqlDialectCompatibilityService();

    @Autowired
    public DatabaseReverseImportServiceImpl(ReverseImportService reverseImportService,
                                            FieldCoverageService fieldCoverageService,
                                            DatabaseMetadataAdapter metadataAdapter) {
        this(reverseImportService, fieldCoverageService, new DriverManagerConnectionProvider(), metadataAdapter);
    }

    public DatabaseReverseImportServiceImpl(ReverseImportService reverseImportService,
                                            ConnectionProvider connectionProvider) {
        this(reverseImportService, null, connectionProvider, new JdbcDatabaseMetadataAdapter());
    }

    public DatabaseReverseImportServiceImpl(ReverseImportService reverseImportService,
                                            FieldCoverageService fieldCoverageService,
                                            ConnectionProvider connectionProvider) {
        this(reverseImportService, fieldCoverageService, connectionProvider, new JdbcDatabaseMetadataAdapter());
    }

    public DatabaseReverseImportServiceImpl(ReverseImportService reverseImportService,
                                            FieldCoverageService fieldCoverageService,
                                            ConnectionProvider connectionProvider,
                                            DatabaseMetadataAdapter metadataAdapter) {
        this.reverseImportService = reverseImportService;
        this.fieldCoverageService = fieldCoverageService;
        this.connectionProvider = connectionProvider;
        this.metadataAdapter = metadataAdapter;
    }

    @Override
    public DatabaseConnectionResult testConnection(DatabaseConnectionReq req) {
        long started = System.nanoTime();
        try (Connection connection = connectionProvider.open(req)) {
            DatabaseConnectionSecurityDiagnostic security = diagnoseConnectionSecurity(connection, req);
            DatabaseConnectionHealthDiagnostic health = connectedHealth(connection, req, security, elapsedMs(started));
            return new DatabaseConnectionResult(true, "连接成功", security, health);
        } catch (SQLException | RuntimeException e) {
            String sanitized = sanitizeConnectionError(e.getMessage(), req);
            return new DatabaseConnectionResult(
                    false,
                    "连接失败: " + sanitized,
                    null,
                    failedHealth(req, e, sanitized, elapsedMs(started)));
        }
    }

    @Override
    public List<DatabaseTableInfo> listTables(DatabaseConnectionReq req) {
        validateConnectionReq(req);
        try (Connection connection = connectionProvider.open(req)) {
            return sanitizeTableInfos(metadataAdapter.listTables(connection, req), req);
        } catch (SQLException e) {
            throw new BizException("读取数据库表失败: " + e.getMessage());
        }
    }

    @Override
    public DatabaseSchemaDump exportDump(DatabaseConnectionReq req) {
        validateConnectionReq(req);
        if (req.getTableNames() == null || req.getTableNames().isEmpty()) {
            throw new BizException("请至少选择一张表");
        }
        try (Connection connection = connectionProvider.open(req)) {
            return sanitizeDump(metadataAdapter.exportDump(connection, req), req);
        } catch (SQLException e) {
            throw new BizException("读取数据库表结构失败: " + e.getMessage());
        }
    }

    @Override
    public DatabaseMetadataBrowser browse(DatabaseConnectionReq req) {
        DatabaseSchemaDump dump = exportDump(req);
        DatabaseSchemaDumpReq dumpReq = new DatabaseSchemaDumpReq();
        dumpReq.setProjectId(req.getProjectId());
        dumpReq.setDump(dump);
        ReverseImportPreview preview = previewDump(dumpReq);
        ReverseImportCompareResult compare = compareDump(dumpReq);
        FieldCoverageReport coverage = coverageDump(dumpReq);
        return buildBrowser(req, dump, preview, compare, coverage);
    }

    @Override
    public ReverseImportPreview preview(DatabaseConnectionReq req) {
        DatabaseSchemaDump dump = exportDump(req);
        ReverseImportPreview preview = reverseImportService.previewTables(req.getProjectId(), metadataAdapter.toTableDefs(req.getProjectId(), dump));
        preview.setDialectDiagnostics(dialectCompatibilityService.diagnoseDatabase(req.getDatabaseType(), req.getSchemaName()));
        return preview;
    }

    @Override
    public ReverseImportPreview previewDump(DatabaseSchemaDumpReq req) {
        return reverseImportService.previewTables(req.getProjectId(), metadataAdapter.toTableDefs(req.getProjectId(), req.getDump()));
    }

    @Override
    public ReverseImportCompareResult compare(DatabaseConnectionReq req) {
        DatabaseSchemaDump dump = exportDump(req);
        return reverseImportService.compareTables(req.getProjectId(), metadataAdapter.toTableDefs(req.getProjectId(), dump));
    }

    @Override
    public ReverseImportCompareResult compareDump(DatabaseSchemaDumpReq req) {
        return reverseImportService.compareTables(req.getProjectId(), metadataAdapter.toTableDefs(req.getProjectId(), req.getDump()));
    }

    @Override
    public FieldCoverageReport coverage(DatabaseConnectionReq req) {
        if (fieldCoverageService == null) {
            throw new BizException("字段覆盖率服务未初始化");
        }
        DatabaseSchemaDump dump = exportDump(req);
        return fieldCoverageService.reportTables(req.getProjectId(), metadataAdapter.toTableDefs(req.getProjectId(), dump));
    }

    @Override
    public FieldCoverageReport coverageDump(DatabaseSchemaDumpReq req) {
        if (fieldCoverageService == null) {
            throw new BizException("字段覆盖率服务未初始化");
        }
        return fieldCoverageService.reportTables(req.getProjectId(), metadataAdapter.toTableDefs(req.getProjectId(), req.getDump()));
    }

    private DatabaseMetadataBrowser buildBrowser(DatabaseConnectionReq req,
                                                  DatabaseSchemaDump dump,
                                                  ReverseImportPreview preview,
                                                  ReverseImportCompareResult compare,
                                                  FieldCoverageReport coverage) {
        Map<String, FieldCandidate> candidates = candidateByColumn(preview);
        Map<String, ReverseImportFieldDiff> diffs = diffByColumn(compare);
        Map<String, FieldCoverageItem> coverageItems = coverageByColumn(coverage);

        DatabaseMetadataBrowser browser = new DatabaseMetadataBrowser();
        browser.setProjectId(req.getProjectId());
        browser.setDatabaseType(dump.getDatabaseType());
        browser.setDatabaseName(dump.getDatabaseName());
        browser.setSchemaName(dump.getSchemaName());
        browser.setSelectedTableNames(dump.getSource() == null
                ? List.of()
                : new ArrayList<>(dump.getSource().getSelectedTableNames() == null
                ? List.of()
                : dump.getSource().getSelectedTableNames()));
        browser.setPreview(preview);
        browser.setCompare(compare);
        browser.setCoverage(coverage);

        for (DatabaseSchemaTable table : dump.getTables()) {
            browser.getTables().add(buildBrowserTable(table, candidates, diffs, coverageItems));
        }
        browser.setSummary(buildBrowserSummary(browser.getTables(), preview, compare, coverage));
        browser.setNextActions(browserNextActions(browser.getSummary()));
        browser.setAiReadableSummary(buildAiReadableSummary(browser, req));
        return browser;
    }

    private DatabaseMetadataBrowserTable buildBrowserTable(DatabaseSchemaTable table,
                                                           Map<String, FieldCandidate> candidates,
                                                           Map<String, ReverseImportFieldDiff> diffs,
                                                           Map<String, FieldCoverageItem> coverageItems) {
        DatabaseMetadataBrowserTable browserTable = new DatabaseMetadataBrowserTable();
        browserTable.setSchemaName(table.getSchemaName());
        browserTable.setTableName(table.getTableName());
        browserTable.setTableType(table.getTableType());
        browserTable.setComment(table.getComment());
        browserTable.setIndexes(new ArrayList<>(table.getIndexes()));
        browserTable.setWarnings(new ArrayList<>(table.getWarnings()));

        for (DatabaseSchemaColumn column : table.getColumns()) {
            String key = metadataKey(table.getTableName(), column.getColumnName());
            DatabaseMetadataBrowserColumn browserColumn = buildBrowserColumn(
                    table,
                    column,
                    candidates.get(key),
                    diffs.get(key),
                    coverageItems.get(key));
            browserTable.getColumns().add(browserColumn);
        }
        browserTable.setColumnCount(browserTable.getColumns().size());
        browserTable.setIndexCount(browserTable.getIndexes().size());
        browserTable.setCandidateCount((int) browserTable.getColumns().stream()
                .filter(DatabaseMetadataBrowserColumn::isImportCandidate)
                .count());
        browserTable.setMissingCommentCount((int) browserTable.getColumns().stream()
                .filter(DatabaseMetadataBrowserColumn::isMissingComment)
                .count());
        browserTable.setChangedCount((int) browserTable.getColumns().stream()
                .filter(DatabaseMetadataBrowserColumn::isTypeChanged)
                .count());
        browserTable.setUnmanagedCount((int) browserTable.getColumns().stream()
                .filter(DatabaseMetadataBrowserColumn::isUnmanaged)
                .count());
        return browserTable;
    }

    private DatabaseMetadataBrowserColumn buildBrowserColumn(DatabaseSchemaTable table,
                                                             DatabaseSchemaColumn column,
                                                             FieldCandidate candidate,
                                                             ReverseImportFieldDiff diff,
                                                             FieldCoverageItem coverageItem) {
        DatabaseMetadataBrowserColumn browserColumn = new DatabaseMetadataBrowserColumn();
        browserColumn.setSchemaName(table.getSchemaName());
        browserColumn.setTableName(table.getTableName());
        browserColumn.setColumnName(column.getColumnName());
        browserColumn.setDataType(column.getDataType());
        browserColumn.setNullable(column.getNullable());
        browserColumn.setDefaultValue(column.getDefaultValue());
        browserColumn.setComment(column.getComment());
        browserColumn.setIndexNames(indexNamesForColumn(table.getIndexes(), column.getColumnName()));
        if (diff != null) {
            browserColumn.setStandardFieldName(diff.getStandardFieldName());
            browserColumn.setStandardDisplayName(diff.getStandardDisplayName());
            browserColumn.setMatchReason(diff.getReason());
            browserColumn.setChanges(new ArrayList<>(diff.getChanges()));
        }
        if (coverageItem != null) {
            if (isBlank(browserColumn.getStandardFieldName())) {
                browserColumn.setStandardFieldName(coverageItem.getStandardFieldName());
            }
            if (isBlank(browserColumn.getStandardDisplayName())) {
                browserColumn.setStandardDisplayName(coverageItem.getStandardDisplayName());
            }
            if (isBlank(browserColumn.getMatchReason())) {
                browserColumn.setMatchReason(coverageItem.getReason());
            }
        }
        if (candidate != null) {
            browserColumn.setImportCandidate(true);
            browserColumn.setSelectedByDefault(true);
            browserColumn.setCandidateKey(candidateKey(candidate));
            browserColumn.setMatchReason(firstNonBlank(candidate.getMatchReason(), browserColumn.getMatchReason()));
        }
        browserColumn.setMissingComment(isBlank(column.getComment())
                || statusEquals(diff, ReverseImportFieldStatus.MISSING_COMMENT)
                || coverageStatusEquals(coverageItem, FieldCoverageStatus.MISSING_COMMENT));
        browserColumn.setTypeChanged(hasChangedDataType(diff));
        browserColumn.setUnmanaged(candidate != null
                || Boolean.TRUE.equals(diff == null ? null : diff.getNonStandard())
                || coverageStatusEquals(coverageItem, FieldCoverageStatus.UNMANAGED));
        browserColumn.setMatchStatus(resolveBrowserStatus(candidate, diff, coverageItem, browserColumn));
        if (isBlank(browserColumn.getCandidateKey())) {
            browserColumn.setCandidateKey(table.getTableName() + "." + column.getColumnName());
        }
        return browserColumn;
    }

    private DatabaseMetadataBrowserSummary buildBrowserSummary(List<DatabaseMetadataBrowserTable> tables,
                                                               ReverseImportPreview preview,
                                                               ReverseImportCompareResult compare,
                                                               FieldCoverageReport coverage) {
        DatabaseMetadataBrowserSummary summary = new DatabaseMetadataBrowserSummary();
        summary.setTableCount(tables.size());
        summary.setColumnCount(tables.stream().mapToInt(DatabaseMetadataBrowserTable::getColumnCount).sum());
        summary.setIndexCount(tables.stream().mapToInt(DatabaseMetadataBrowserTable::getIndexCount).sum());
        ReverseImportSummary previewSummary = preview == null ? null : preview.getSummary();
        ReverseImportCompareSummary compareSummary = compare == null ? null : compare.getSummary();
        summary.setCandidateCount(valueOrZero(previewSummary == null ? null : previewSummary.getCandidateCount()));
        summary.setMissingCommentCount(valueOrZero(compareSummary == null ? null : compareSummary.getMissingCommentCount()));
        summary.setChangedCount(valueOrZero(compareSummary == null ? null : compareSummary.getChangedCount()));
        summary.setUnmanagedCount(coverage == null ? 0 : coverage.getSummary().getUnmanagedCount());
        return summary;
    }

    private Map<String, FieldCandidate> candidateByColumn(ReverseImportPreview preview) {
        Map<String, FieldCandidate> values = new LinkedHashMap<>();
        if (preview == null || preview.getFieldCandidates() == null) {
            return values;
        }
        for (FieldCandidate candidate : preview.getFieldCandidates()) {
            values.put(metadataKey(candidate.getTableName(), candidate.getColumnName()), candidate);
        }
        return values;
    }

    private Map<String, ReverseImportFieldDiff> diffByColumn(ReverseImportCompareResult compare) {
        Map<String, ReverseImportFieldDiff> values = new LinkedHashMap<>();
        if (compare == null || compare.getTableDiffs() == null) {
            return values;
        }
        for (ReverseImportTableDiff tableDiff : compare.getTableDiffs()) {
            for (ReverseImportFieldDiff diff : tableDiff.getFieldDiffs()) {
                values.put(metadataKey(diff.getTableName(), diff.getColumnName()), diff);
            }
        }
        return values;
    }

    private Map<String, FieldCoverageItem> coverageByColumn(FieldCoverageReport coverage) {
        Map<String, FieldCoverageItem> values = new LinkedHashMap<>();
        if (coverage == null || coverage.getTables() == null) {
            return values;
        }
        for (FieldCoverageTable table : coverage.getTables()) {
            for (FieldCoverageItem item : table.getFields()) {
                values.put(metadataKey(item.getTableName(), item.getColumnName()), item);
            }
        }
        return values;
    }

    private List<String> indexNamesForColumn(List<DatabaseSchemaIndex> indexes, String columnName) {
        List<String> names = new ArrayList<>();
        for (DatabaseSchemaIndex index : indexes) {
            if (equalsIgnoreCase(index.getColumnName(), columnName) && !isBlank(index.getIndexName())) {
                names.add(index.getIndexName());
            }
        }
        return names;
    }

    private String resolveBrowserStatus(FieldCandidate candidate,
                                        ReverseImportFieldDiff diff,
                                        FieldCoverageItem coverageItem,
                                        DatabaseMetadataBrowserColumn column) {
        if (candidate != null) {
            return "NEW";
        }
        if (diff != null && diff.getStatus() != null) {
            return diff.getStatus().name();
        }
        if (coverageItem != null && coverageItem.getStatus() != null) {
            return coverageItem.getStatus().name();
        }
        if (column.isMissingComment()) {
            return "MISSING_COMMENT";
        }
        return "UNKNOWN";
    }

    private List<String> browserNextActions(DatabaseMetadataBrowserSummary summary) {
        List<String> actions = new ArrayList<>();
        actions.add("可继续生成候选导入预览、标准差异比对或覆盖率报告。");
        if (summary.getCandidateCount() > 0) {
            actions.add("确认候选字段后，可使用既有反向导入确认流程写入标准字段库。");
        }
        if (summary.getMissingCommentCount() > 0) {
            actions.add("建议优先补齐缺失注释，提升 AI Context 和数据字典可读性。");
        }
        return actions;
    }

    private String buildAiReadableSummary(DatabaseMetadataBrowser browser, DatabaseConnectionReq req) {
        StringBuilder text = new StringBuilder();
        text.append("DataSpec database metadata browser\n");
        text.append("projectId=").append(browser.getProjectId()).append('\n');
        text.append("database=").append(browser.getDatabaseType()).append('/')
                .append(browser.getDatabaseName()).append('/')
                .append(browser.getSchemaName()).append('\n');
        DatabaseMetadataBrowserSummary summary = browser.getSummary();
        text.append("summary: tables=").append(summary.getTableCount())
                .append(", columns=").append(summary.getColumnCount())
                .append(", indexes=").append(summary.getIndexCount())
                .append(", candidates=").append(summary.getCandidateCount())
                .append(", missingComments=").append(summary.getMissingCommentCount())
                .append(", changed=").append(summary.getChangedCount())
                .append(", unmanaged=").append(summary.getUnmanagedCount())
                .append('\n');
        for (DatabaseMetadataBrowserTable table : browser.getTables()) {
            text.append("- table ").append(table.getTableName());
            if (!isBlank(table.getComment())) {
                text.append(" # ").append(table.getComment());
            }
            text.append('\n');
            if (!table.getIndexes().isEmpty()) {
                text.append("  indexes: ");
                text.append(String.join(", ", table.getIndexes().stream()
                        .map(index -> index.getIndexName() + "(" + index.getColumnName() + ")")
                        .toList()));
                text.append('\n');
            }
            for (DatabaseMetadataBrowserColumn column : table.getColumns()) {
                text.append("  - ").append(column.getColumnName())
                        .append(' ').append(column.getDataType())
                        .append(" status=").append(column.getMatchStatus());
                if (!isBlank(column.getStandardFieldName())) {
                    text.append(" standard=").append(column.getStandardFieldName());
                }
                if (column.isImportCandidate()) {
                    text.append(" candidateKey=").append(column.getCandidateKey());
                }
                if (!isBlank(column.getComment())) {
                    text.append(" comment=").append(column.getComment());
                }
                text.append('\n');
            }
        }
        return SensitiveDataSanitizer.redactText(text.toString(), 8000, req == null ? null : req.getPassword());
    }

    private List<DatabaseTableInfo> sanitizeTableInfos(List<DatabaseTableInfo> tables, DatabaseConnectionReq req) {
        if (tables == null) {
            return List.of();
        }
        List<DatabaseTableInfo> sanitized = new ArrayList<>(tables.size());
        for (DatabaseTableInfo table : tables) {
            sanitized.add(new DatabaseTableInfo(
                    sanitizeMetadataText(table.schemaName(), req),
                    sanitizeMetadataText(table.tableName(), req),
                    sanitizeMetadataText(table.tableType(), req),
                    sanitizeMetadataText(table.comment(), req)));
        }
        return sanitized;
    }

    private DatabaseSchemaDump sanitizeDump(DatabaseSchemaDump dump, DatabaseConnectionReq req) {
        if (dump == null) {
            return null;
        }
        dump.setDatabaseType(sanitizeMetadataText(dump.getDatabaseType(), req));
        dump.setDatabaseName(sanitizeMetadataText(dump.getDatabaseName(), req));
        dump.setSchemaName(sanitizeMetadataText(dump.getSchemaName(), req));
        sanitizeDumpSource(dump.getSource(), req);
        for (DatabaseSchemaTable table : dump.getTables()) {
            sanitizeDumpTable(table, req);
        }
        dump.setWarnings(sanitizeMetadataTexts(dump.getWarnings(), req));
        return dump;
    }

    private void sanitizeDumpSource(DatabaseSchemaSource source, DatabaseConnectionReq req) {
        if (source == null) {
            return;
        }
        source.setSourceType(sanitizeMetadataText(source.getSourceType(), req));
        source.setDatabaseProductName(sanitizeMetadataText(source.getDatabaseProductName(), req));
        source.setDatabaseProductVersion(sanitizeMetadataText(source.getDatabaseProductVersion(), req));
        source.setCatalogName(sanitizeMetadataText(source.getCatalogName(), req));
        source.setSchemaName(sanitizeMetadataText(source.getSchemaName(), req));
        source.setSelectedTableNames(sanitizeMetadataTexts(source.getSelectedTableNames(), req));
    }

    private void sanitizeDumpTable(DatabaseSchemaTable table, DatabaseConnectionReq req) {
        if (table == null) {
            return;
        }
        table.setSchemaName(sanitizeMetadataText(table.getSchemaName(), req));
        table.setTableName(sanitizeMetadataText(table.getTableName(), req));
        table.setTableType(sanitizeMetadataText(table.getTableType(), req));
        table.setComment(sanitizeMetadataText(table.getComment(), req));
        for (DatabaseSchemaColumn column : table.getColumns()) {
            sanitizeDumpColumn(column, req);
        }
        for (DatabaseSchemaIndex index : table.getIndexes()) {
            sanitizeDumpIndex(index, req);
        }
        table.setWarnings(sanitizeMetadataTexts(table.getWarnings(), req));
    }

    private void sanitizeDumpColumn(DatabaseSchemaColumn column, DatabaseConnectionReq req) {
        if (column == null) {
            return;
        }
        column.setColumnName(sanitizeMetadataText(column.getColumnName(), req));
        column.setDataType(sanitizeMetadataText(column.getDataType(), req));
        column.setDefaultValue(sanitizeMetadataText(column.getDefaultValue(), req));
        column.setComment(sanitizeMetadataText(column.getComment(), req));
    }

    private void sanitizeDumpIndex(DatabaseSchemaIndex index, DatabaseConnectionReq req) {
        if (index == null) {
            return;
        }
        index.setSchemaName(sanitizeMetadataText(index.getSchemaName(), req));
        index.setTableName(sanitizeMetadataText(index.getTableName(), req));
        index.setIndexName(sanitizeMetadataText(index.getIndexName(), req));
        index.setColumnName(sanitizeMetadataText(index.getColumnName(), req));
    }

    private List<String> sanitizeMetadataTexts(List<String> values, DatabaseConnectionReq req) {
        if (values == null) {
            return new ArrayList<>();
        }
        List<String> sanitized = new ArrayList<>(values.size());
        for (String value : values) {
            sanitized.add(sanitizeMetadataText(value, req));
        }
        return sanitized;
    }

    private String sanitizeMetadataText(String value, DatabaseConnectionReq req) {
        return SensitiveDataSanitizer.redactText(value, 1000, req == null ? null : req.getPassword());
    }

    private String metadataKey(String tableName, String columnName) {
        return normalizeKeyPart(tableName) + "." + normalizeKeyPart(columnName);
    }

    private String normalizeKeyPart(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private String candidateKey(FieldCandidate candidate) {
        return (candidate.getTableName() == null ? "" : candidate.getTableName())
                + "."
                + (candidate.getColumnName() == null ? "" : candidate.getColumnName());
    }

    private boolean statusEquals(ReverseImportFieldDiff diff, ReverseImportFieldStatus status) {
        return diff != null && diff.getStatus() == status;
    }

    private boolean coverageStatusEquals(FieldCoverageItem item, FieldCoverageStatus status) {
        return item != null && item.getStatus() == status;
    }

    private boolean hasChangedDataType(ReverseImportFieldDiff diff) {
        if (diff == null) {
            return false;
        }
        if (diff.getStatus() == ReverseImportFieldStatus.CHANGED) {
            return true;
        }
        return diff.getChanges().stream()
                .anyMatch(change -> "dataType".equalsIgnoreCase(change.getProperty()));
    }

    private boolean equalsIgnoreCase(String left, String right) {
        return left != null && right != null && left.equalsIgnoreCase(right);
    }

    private int valueOrZero(Integer value) {
        return value == null ? 0 : value;
    }

    private String firstNonBlank(String first, String second) {
        return isBlank(first) ? second : first;
    }

    private DatabaseConnectionSecurityDiagnostic diagnoseConnectionSecurity(Connection connection,
                                                                            DatabaseConnectionReq req) {
        String type = databaseType(req);
        List<String> warnings = new ArrayList<>();
        List<String> actions = new ArrayList<>();
        List<String> sql = recommendedSql(type, req);
        String currentUser = null;
        Boolean readOnly = null;
        Boolean writeRisk = null;
        int schemaCount = 0;
        int tableCount = 0;

        try {
            DatabaseMetaData metaData = connection.getMetaData();
            currentUser = sanitizeConnectionError(metaData.getUserName(), req);
            readOnly = knownReadOnly(readOnly, metaData.isReadOnly());
            schemaCount = countSchemas(metaData, req);
            tableCount = countTables(metaData, req);
        } catch (SQLException e) {
            warnings.add("无法完整读取 JDBC metadata: " + sanitizeConnectionError(e.getMessage(), req));
        }

        try {
            readOnly = knownReadOnly(readOnly, connection.isReadOnly());
        } catch (SQLException e) {
            warnings.add("无法读取连接只读状态: " + sanitizeConnectionError(e.getMessage(), req));
        }

        Map<String, Object> dialectSignals = readDialectSignals(connection, type, warnings, req);
        if (dialectSignals.containsKey("currentUser")) {
            currentUser = (String) dialectSignals.get("currentUser");
        }
        if (dialectSignals.containsKey("readOnly")) {
            readOnly = (Boolean) dialectSignals.get("readOnly");
        }
        if (dialectSignals.containsKey("writeRisk")) {
            writeRisk = (Boolean) dialectSignals.get("writeRisk");
        }

        if (!TYPE_POSTGRESQL.equals(type) && !TYPE_MYSQL.equals(type)) {
            warnings.add("暂不支持该数据库类型的权限诊断，只能确认连接可用。");
        } else if (writeRisk == null && readOnly == null) {
            warnings.add("连接可用，但无法确认账号是否只读，请优先使用专用只读账号。");
        } else if (Boolean.TRUE.equals(writeRisk)) {
            warnings.add("当前账号可能具备写入或建库建表相关权限，建议切换只读账号。");
        } else if (!Boolean.TRUE.equals(readOnly)) {
            warnings.add("连接未明确标记为只读，请确认该账号仅授予 metadata 和 SELECT 权限。");
        }

        String riskLevel = resolveRiskLevel(type, readOnly, writeRisk, warnings);
        actions.addAll(recommendedActions(type, riskLevel));
        return new DatabaseConnectionSecurityDiagnostic(
                normalizeDiagnosticType(type),
                currentUser,
                readOnly,
                writeRisk,
                riskLevel,
                schemaCount,
                tableCount,
                warnings,
                actions,
                sql);
    }

    private Map<String, Object> readDialectSignals(Connection connection,
                                                   String type,
                                                   List<String> warnings,
                                                   DatabaseConnectionReq req) {
        Map<String, Object> signals = new LinkedHashMap<>();
        if (!TYPE_POSTGRESQL.equals(type) && !TYPE_MYSQL.equals(type)) {
            return signals;
        }
        try (Statement statement = connection.createStatement()) {
            if (TYPE_POSTGRESQL.equals(type)) {
                readPostgresqlSignals(statement, signals);
            } else {
                readMysqlSignals(statement, signals);
            }
        } catch (SQLException e) {
            warnings.add("权限诊断查询失败: " + sanitizeConnectionError(e.getMessage(), req));
        }
        return signals;
    }

    private void readPostgresqlSignals(Statement statement, Map<String, Object> signals) throws SQLException {
        String currentUser = queryString(statement, "select current_user");
        if (!isBlank(currentUser)) {
            signals.put("currentUser", currentUser);
        }
        String transactionReadOnly = queryString(statement, "select current_setting('transaction_read_only', true)");
        if (!isBlank(transactionReadOnly)) {
            signals.put("readOnly", "on".equalsIgnoreCase(transactionReadOnly)
                    || "true".equalsIgnoreCase(transactionReadOnly));
        }
        boolean canCreateDatabase = queryBoolean(statement,
                "select has_database_privilege(current_database(), 'CREATE')");
        signals.put("writeRisk", canCreateDatabase);
    }

    private void readMysqlSignals(Statement statement, Map<String, Object> signals) throws SQLException {
        String currentUser = queryString(statement, "select current_user()");
        if (!isBlank(currentUser)) {
            signals.put("currentUser", currentUser);
        }
        Boolean readOnly = mysqlReadOnly(statement, "@@read_only");
        Boolean superReadOnly = mysqlReadOnly(statement, "@@super_read_only");
        if (readOnly != null && superReadOnly != null) {
            signals.put("readOnly", readOnly && superReadOnly);
        } else if (readOnly != null) {
            signals.put("readOnly", readOnly);
        }
        String grants = queryJoinedStrings(statement, "show grants for current_user()");
        if (!isBlank(grants)) {
            boolean hasWritePrivilege = containsMysqlWritePrivilege(grants);
            signals.put("writeRisk", hasWritePrivilege);
            if (!hasWritePrivilege) {
                signals.put("readOnly", true);
            }
        } else if (readOnly != null && !readOnly) {
            // 没有 grants 时不能证明账号可写，只提示风险，不执行写探测。
            signals.put("writeRisk", false);
        }
    }

    private Boolean mysqlReadOnly(Statement statement, String variableName) throws SQLException {
        String value = queryString(statement, "select " + variableName);
        if (isBlank(value)) {
            return null;
        }
        return "1".equals(value) || "on".equalsIgnoreCase(value) || "true".equalsIgnoreCase(value);
    }

    private boolean containsMysqlWritePrivilege(String grants) {
        String normalized = grants.toUpperCase(Locale.ROOT);
        return normalized.contains("ALL PRIVILEGES")
                || normalized.contains(" INSERT")
                || normalized.contains(" UPDATE")
                || normalized.contains(" DELETE")
                || normalized.contains(" CREATE")
                || normalized.contains(" ALTER")
                || normalized.contains(" DROP");
    }

    private String queryString(Statement statement, String sql) throws SQLException {
        try (ResultSet rs = statement.executeQuery(sql)) {
            return rs.next() ? rs.getString(1) : null;
        }
    }

    private boolean queryBoolean(Statement statement, String sql) throws SQLException {
        try (ResultSet rs = statement.executeQuery(sql)) {
            return rs.next() && rs.getBoolean(1);
        }
    }

    private String queryJoinedStrings(Statement statement, String sql) throws SQLException {
        List<String> values = new ArrayList<>();
        try (ResultSet rs = statement.executeQuery(sql)) {
            while (rs.next()) {
                values.add(rs.getString(1));
            }
        }
        return String.join("\n", values);
    }

    private int countSchemas(DatabaseMetaData metaData, DatabaseConnectionReq req) throws SQLException {
        if (TYPE_MYSQL.equals(databaseType(req))) {
            try (ResultSet rs = metaData.getCatalogs()) {
                return countRows(rs);
            }
        }
        try (ResultSet rs = metaData.getSchemas()) {
            return countRows(rs);
        }
    }

    private int countTables(DatabaseMetaData metaData, DatabaseConnectionReq req) throws SQLException {
        try (ResultSet rs = metaData.getTables(catalog(req), schemaPattern(req), "%", new String[]{"TABLE"})) {
            return countRows(rs);
        }
    }

    private int countRows(ResultSet rs) throws SQLException {
        int count = 0;
        while (rs.next()) {
            count++;
        }
        return count;
    }

    private Boolean knownReadOnly(Boolean current, boolean candidate) {
        if (current == null) {
            return candidate;
        }
        return current || candidate;
    }

    private String resolveRiskLevel(String type, Boolean readOnly, Boolean writeRisk, List<String> warnings) {
        if (!TYPE_POSTGRESQL.equals(type) && !TYPE_MYSQL.equals(type)) {
            return RISK_UNKNOWN;
        }
        if (Boolean.TRUE.equals(writeRisk)) {
            return RISK_DANGER;
        }
        if (Boolean.TRUE.equals(readOnly) && warnings.isEmpty()) {
            return RISK_SAFE;
        }
        return RISK_WARNING;
    }

    private List<String> recommendedActions(String type, String riskLevel) {
        List<String> actions = new ArrayList<>();
        if (RISK_SAFE.equals(riskLevel)) {
            actions.add("当前连接适合用于 DataSpec 反向导入、二次比对和覆盖率报告。");
        } else if (RISK_DANGER.equals(riskLevel)) {
            actions.add("建议立即切换为专用只读账号，避免 AI 或人工误用高权限连接。");
        } else {
            actions.add("建议使用专用只读账号，并限制为 metadata 读取和 SELECT 权限。");
        }
        if (TYPE_POSTGRESQL.equals(type)) {
            actions.add("PostgreSQL 建议按 schema 授予 USAGE 和 SELECT，不授予 CREATE/ALTER/DROP。");
        } else if (TYPE_MYSQL.equals(type)) {
            actions.add("MySQL 建议只授予目标库 SELECT、SHOW VIEW，不授予 INSERT/UPDATE/DELETE/DDL 权限。");
        } else {
            actions.add("当前数据库类型暂未内置最小权限 SQL，请按只读账号原则手动配置。");
        }
        return actions;
    }

    private List<String> recommendedSql(String type, DatabaseConnectionReq req) {
        if (TYPE_POSTGRESQL.equals(type)) {
            String database = safeIdentifier(req.getDatabaseName(), "your_database");
            String schema = safeIdentifier(schemaPattern(req), "public");
            return List.of(
                    "CREATE ROLE dataspec_ro LOGIN PASSWORD '<password>';",
                    "GRANT CONNECT ON DATABASE " + database + " TO dataspec_ro;",
                    "GRANT USAGE ON SCHEMA " + schema + " TO dataspec_ro;",
                    "GRANT SELECT ON ALL TABLES IN SCHEMA " + schema + " TO dataspec_ro;",
                    "ALTER DEFAULT PRIVILEGES IN SCHEMA " + schema + " GRANT SELECT ON TABLES TO dataspec_ro;");
        }
        if (TYPE_MYSQL.equals(type)) {
            String database = safeIdentifier(req.getDatabaseName(), "your_database");
            return List.of(
                    "CREATE USER 'dataspec_ro'@'%' IDENTIFIED BY '<password>';",
                    "GRANT SELECT, SHOW VIEW ON `" + database + "`.* TO 'dataspec_ro'@'%';");
        }
        return List.of();
    }

    private DatabaseConnectionHealthDiagnostic connectedHealth(Connection connection,
                                                              DatabaseConnectionReq req,
                                                              DatabaseConnectionSecurityDiagnostic security,
                                                              long latencyMs) {
        String type = databaseType(req);
        List<String> warnings = new ArrayList<>();
        String product = null;
        String version = null;
        try {
            DatabaseMetaData metaData = connection.getMetaData();
            product = sanitizeConnectionError(metaData.getDatabaseProductName(), req);
            version = sanitizeConnectionError(metaData.getDatabaseProductVersion(), req);
        } catch (SQLException e) {
            warnings.add("无法读取数据库产品信息: " + sanitizeConnectionError(e.getMessage(), req));
        }
        if (security != null && security.warnings() != null) {
            warnings.addAll(security.warnings());
        }
        DatabaseDialectCapability capability = dialectCapability(type, product != null || version != null);
        String readonlyCheck = security == null ? "UNKNOWN" : security.riskLevel();
        return new DatabaseConnectionHealthDiagnostic(
                STATUS_CONNECTED,
                latencyMs,
                product,
                version,
                normalizeDiagnosticType(type),
                null,
                null,
                "连接成功",
                capability,
                readonlyCheck,
                requiredPrivileges(type),
                warnings,
                connectedNextActions(type, readonlyCheck));
    }

    private DatabaseConnectionHealthDiagnostic failedHealth(DatabaseConnectionReq req,
                                                           Exception error,
                                                           String sanitizedMessage,
                                                           long latencyMs) {
        String type = databaseType(req);
        String category = failureCategory(type, error == null ? null : error.getMessage());
        return new DatabaseConnectionHealthDiagnostic(
                STATUS_FAILED,
                latencyMs,
                null,
                null,
                normalizeDiagnosticType(type),
                category,
                retryable(category),
                sanitizedMessage,
                dialectCapability(type, false),
                "UNKNOWN",
                requiredPrivileges(type),
                failureWarnings(category, sanitizedMessage),
                failureNextActions(category));
    }

    private DatabaseDialectCapability dialectCapability(String type, boolean metadataReadable) {
        if (TYPE_POSTGRESQL.equals(type)) {
            return new DatabaseDialectCapability(
                    "POSTGRESQL",
                    "SUPPORTED",
                    "SUPPORTED",
                    "SUPPORTED",
                    metadataReadable,
                    List.of("reverse-import", "compare", "coverage", "dump"));
        }
        if (TYPE_MYSQL.equals(type)) {
            return new DatabaseDialectCapability(
                    "MYSQL",
                    "SUPPORTED",
                    "SUPPORTED",
                    "SUPPORTED",
                    metadataReadable,
                    List.of("reverse-import", "compare", "coverage", "dump"));
        }
        return new DatabaseDialectCapability(
                normalizeDiagnosticType(type),
                "UNSUPPORTED",
                "UNSUPPORTED",
                "UNSUPPORTED",
                false,
                List.of());
    }

    private List<String> requiredPrivileges(String type) {
        if (TYPE_POSTGRESQL.equals(type)) {
            return List.of("CONNECT", "USAGE", "SELECT");
        }
        if (TYPE_MYSQL.equals(type)) {
            return List.of("SELECT", "SHOW VIEW");
        }
        return List.of("只读 metadata 读取权限");
    }

    private List<String> connectedNextActions(String type, String readonlyCheck) {
        List<String> actions = new ArrayList<>();
        actions.add("可以继续加载表、反向导入、二次比对或覆盖率报告。");
        if (!RISK_SAFE.equals(readonlyCheck)) {
            actions.add("连接可用但只读安全性未完全确认，建议优先切换专用只读账号。");
        }
        if (!TYPE_POSTGRESQL.equals(type) && !TYPE_MYSQL.equals(type)) {
            actions.add("当前方言能力未内置，建议改用 PostgreSQL/MySQL 或停止直连流程。");
        }
        return actions;
    }

    private String failureCategory(String type, String rawMessage) {
        if (!TYPE_POSTGRESQL.equals(type) && !TYPE_MYSQL.equals(type)) {
            return FAILURE_UNSUPPORTED_DIALECT;
        }
        String message = rawMessage == null ? "" : rawMessage.toLowerCase(Locale.ROOT);
        if (message.contains("password")
                || message.contains("authentication")
                || message.contains("access denied")
                || message.contains("login")
                || message.contains("认证")) {
            return FAILURE_AUTHENTICATION;
        }
        if (message.contains("permission denied")
                || message.contains("privilege")
                || message.contains("not allowed")
                || message.contains("权限")) {
            return FAILURE_PERMISSION_DENIED;
        }
        if (message.contains("unknown database")
                || message.contains("database") && message.contains("does not exist")
                || message.contains("schema") && message.contains("does not exist")
                || message.contains("catalog")) {
            return FAILURE_SCHEMA_NOT_FOUND;
        }
        if (message.contains("connection refused")
                || message.contains("timed out")
                || message.contains("timeout")
                || message.contains("unknown host")
                || message.contains("network")
                || message.contains("no route")
                || message.contains("could not connect")) {
            return FAILURE_NETWORK;
        }
        return FAILURE_UNKNOWN;
    }

    private boolean retryable(String category) {
        return !FAILURE_UNSUPPORTED_DIALECT.equals(category);
    }

    private List<String> failureWarnings(String category, String sanitizedMessage) {
        if (FAILURE_UNKNOWN.equals(category)) {
            return List.of("连接失败原因未能精确分类: " + sanitizedMessage);
        }
        return List.of();
    }

    private List<String> failureNextActions(String category) {
        return switch (category) {
            case FAILURE_AUTHENTICATION -> List.of("检查用户名或密码是否正确，确认账号未过期或被锁定。");
            case FAILURE_NETWORK -> List.of("确认主机、端口、防火墙、VPN 或容器网络是否可达。");
            case FAILURE_SCHEMA_NOT_FOUND -> List.of("确认数据库名、schema/catalog 名称和大小写是否正确。");
            case FAILURE_PERMISSION_DENIED -> List.of("为连接账号授予只读 metadata 与 SELECT 权限，或切换专用只读账号。");
            case FAILURE_UNSUPPORTED_DIALECT -> List.of("当前只支持 PostgreSQL/MySQL 直连诊断，请选择受支持的数据库类型。");
            default -> List.of("查看数据库返回的脱敏错误信息，修复连接配置后重试。");
        };
    }

    private long elapsedMs(long started) {
        return Math.max(0, (System.nanoTime() - started) / 1_000_000);
    }

    private String normalizeDiagnosticType(String type) {
        if (TYPE_POSTGRESQL.equals(type)) {
            return "POSTGRESQL";
        }
        if (TYPE_MYSQL.equals(type)) {
            return "MYSQL";
        }
        return isBlank(type) ? "UNKNOWN" : type.toUpperCase(Locale.ROOT);
    }

    private String safeIdentifier(String value, String fallback) {
        if (isBlank(value)) {
            return fallback;
        }
        return value.replaceAll("[^A-Za-z0-9_]", "_");
    }

    private String sanitizeConnectionError(String message, DatabaseConnectionReq req) {
        if (message == null) {
            return "未知错误";
        }
        return SensitiveDataSanitizer.redactText(message, 500, req == null ? null : req.getPassword());
    }

    private String catalog(DatabaseConnectionReq req) {
        return TYPE_MYSQL.equals(databaseType(req)) ? req.getDatabaseName() : null;
    }

    private String schemaPattern(DatabaseConnectionReq req) {
        if (TYPE_MYSQL.equals(databaseType(req))) {
            return null;
        }
        if (req.getSchemaName() == null || req.getSchemaName().isBlank()) {
            return "public";
        }
        return req.getSchemaName();
    }

    private void validateConnectionReq(DatabaseConnectionReq req) {
        if (req == null || req.getProjectId() == null) {
            throw new BizException("项目ID不能为空");
        }
        if (isBlank(req.getDatabaseType()) || isBlank(req.getHost()) || isBlank(req.getDatabaseName())
                || isBlank(req.getUsername())) {
            throw new BizException("数据库连接信息不完整");
        }
        String type = databaseType(req);
        if (!TYPE_POSTGRESQL.equals(type) && !TYPE_MYSQL.equals(type)) {
            throw new BizException("暂不支持的数据库类型: " + req.getDatabaseType());
        }
    }

    private String databaseType(DatabaseConnectionReq req) {
        return req.getDatabaseType() == null ? "" : req.getDatabaseType().trim().toLowerCase(Locale.ROOT);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public interface ConnectionProvider {
        Connection open(DatabaseConnectionReq req) throws SQLException;
    }

    private static class DriverManagerConnectionProvider implements ConnectionProvider {

        @Override
        public Connection open(DatabaseConnectionReq req) throws SQLException {
            String type = req.getDatabaseType().trim().toLowerCase(Locale.ROOT);
            int port = req.getPort() != null ? req.getPort() : defaultPort(type);
            String url = switch (type) {
                case TYPE_POSTGRESQL -> "jdbc:postgresql://" + req.getHost() + ":" + port + "/" + req.getDatabaseName();
                case TYPE_MYSQL -> "jdbc:mysql://" + req.getHost() + ":" + port + "/" + req.getDatabaseName()
                        + "?useInformationSchema=true&remarks=true";
                default -> throw new SQLException("暂不支持的数据库类型: " + req.getDatabaseType());
            };
            Properties properties = new Properties();
            properties.setProperty("user", req.getUsername());
            properties.setProperty("password", req.getPassword() == null ? "" : req.getPassword());
            return DriverManager.getConnection(url, properties);
        }

        private int defaultPort(String type) throws SQLException {
            return switch (type) {
                case TYPE_POSTGRESQL -> 5432;
                case TYPE_MYSQL -> 3306;
                default -> throw new SQLException("暂不支持的数据库类型: " + type);
            };
        }
    }
}
