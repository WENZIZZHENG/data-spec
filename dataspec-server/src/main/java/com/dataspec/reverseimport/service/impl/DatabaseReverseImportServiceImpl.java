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
import com.dataspec.reverseimport.model.DatabaseMetadataCacheInfo;
import com.dataspec.reverseimport.model.DatabaseMetadataCacheMode;
import com.dataspec.reverseimport.model.DatabaseSchemaDump;
import com.dataspec.reverseimport.model.DatabaseSchemaDumpReq;
import com.dataspec.reverseimport.model.DatabaseMetadataBrowser;
import com.dataspec.reverseimport.model.DatabaseMetadataBrowserColumn;
import com.dataspec.reverseimport.model.DatabaseMetadataBrowserSummary;
import com.dataspec.reverseimport.model.DatabaseMetadataBrowserTable;
import com.dataspec.reverseimport.model.DatabaseMetadataScanProgress;
import com.dataspec.reverseimport.model.DatabaseMetadataScanReq;
import com.dataspec.reverseimport.model.DatabaseMetadataScanResult;
import com.dataspec.reverseimport.model.DatabaseMetadataScanSummary;
import com.dataspec.reverseimport.model.DatabaseSchemaChangeAction;
import com.dataspec.reverseimport.model.DatabaseSchemaChangeItem;
import com.dataspec.reverseimport.model.DatabaseSchemaChangePlan;
import com.dataspec.reverseimport.model.DatabaseSchemaChangeSummary;
import com.dataspec.reverseimport.model.DatabaseSchemaColumn;
import com.dataspec.reverseimport.model.DatabaseSchemaIndex;
import com.dataspec.reverseimport.model.DatabaseSchemaSource;
import com.dataspec.reverseimport.model.DatabaseSchemaTable;
import com.dataspec.reverseimport.model.DatabaseTableInfo;
import com.dataspec.reverseimport.model.FieldCandidate;
import com.dataspec.reverseimport.model.ReverseImportCompareResult;
import com.dataspec.reverseimport.model.ReverseImportCompareSummary;
import com.dataspec.reverseimport.model.ReverseImportFieldChange;
import com.dataspec.reverseimport.model.ReverseImportFieldDiff;
import com.dataspec.reverseimport.model.ReverseImportFieldStatus;
import com.dataspec.reverseimport.model.ReverseImportPreview;
import com.dataspec.reverseimport.model.ReverseImportSummary;
import com.dataspec.reverseimport.model.ReverseImportTableDiff;
import com.dataspec.reverseimport.service.DatabaseMetadataAdapter;
import com.dataspec.reverseimport.service.DatabaseMetadataCacheService;
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
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

/**
 * 基于 JDBC metadata 的数据库直连反向导入。
 */
@Service
public class DatabaseReverseImportServiceImpl implements DatabaseReverseImportService {

    private static final String TYPE_POSTGRESQL = "postgresql";
    private static final String TYPE_MYSQL = "mysql";
    private static final String RISK_SAFE = "SAFE";
    private static final String SCHEMA_RISK_LOW = "LOW";
    private static final String SCHEMA_RISK_MEDIUM = "MEDIUM";
    private static final String SCHEMA_RISK_HIGH = "HIGH";
    private static final String SCHEMA_RISK_BLOCKED = "BLOCKED";
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
    private static final int DEFAULT_SCAN_PAGE_SIZE = 50;
    private static final int MAX_SCAN_PAGE_SIZE = 100;

    private final ReverseImportService reverseImportService;
    private final FieldCoverageService fieldCoverageService;
    private final ConnectionProvider connectionProvider;
    private final DatabaseMetadataAdapter metadataAdapter;
    private final DatabaseMetadataCacheService metadataCacheService;
    private final SqlDialectCompatibilityService dialectCompatibilityService = new SqlDialectCompatibilityService();

    @Autowired
    public DatabaseReverseImportServiceImpl(ReverseImportService reverseImportService,
                                            FieldCoverageService fieldCoverageService,
                                            DatabaseMetadataAdapter metadataAdapter,
                                            DatabaseMetadataCacheService metadataCacheService) {
        this(reverseImportService, fieldCoverageService, new DriverManagerConnectionProvider(), metadataAdapter, metadataCacheService);
    }

    public DatabaseReverseImportServiceImpl(ReverseImportService reverseImportService,
                                            ConnectionProvider connectionProvider) {
        this(reverseImportService, null, connectionProvider, new JdbcDatabaseMetadataAdapter(), null);
    }

    public DatabaseReverseImportServiceImpl(ReverseImportService reverseImportService,
                                            FieldCoverageService fieldCoverageService,
                                            ConnectionProvider connectionProvider) {
        this(reverseImportService, fieldCoverageService, connectionProvider, new JdbcDatabaseMetadataAdapter(), null);
    }

    public DatabaseReverseImportServiceImpl(ReverseImportService reverseImportService,
                                            FieldCoverageService fieldCoverageService,
                                            ConnectionProvider connectionProvider,
                                            DatabaseMetadataAdapter metadataAdapter) {
        this(reverseImportService, fieldCoverageService, connectionProvider, metadataAdapter, null);
    }

    public DatabaseReverseImportServiceImpl(ReverseImportService reverseImportService,
                                            FieldCoverageService fieldCoverageService,
                                            ConnectionProvider connectionProvider,
                                            DatabaseMetadataAdapter metadataAdapter,
                                            DatabaseMetadataCacheService metadataCacheService) {
        this.reverseImportService = reverseImportService;
        this.fieldCoverageService = fieldCoverageService;
        this.connectionProvider = connectionProvider;
        this.metadataAdapter = metadataAdapter;
        this.metadataCacheService = metadataCacheService == null
                ? NoopDatabaseMetadataCacheService.INSTANCE
                : metadataCacheService;
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
            throw new BizException("读取数据库表失败: " + sanitizeConnectionError(e.getMessage(), req));
        }
    }

    @Override
    public DatabaseMetadataScanResult scan(DatabaseMetadataScanReq req) {
        validateConnectionReq(req);
        int pageSize = scanPageSize(req.getPageSize());
        int offset = scanOffset(req.getCursor());
        String scanId = scanId(req);
        if (Boolean.TRUE.equals(req.getCancel())) {
            return cancelledScanResult(req, scanId, pageSize, offset);
        }

        List<DatabaseTableInfo> tables = new ArrayList<>(listTables(req));
        tables.sort(Comparator
                .comparing((DatabaseTableInfo table) -> textForSort(table.schemaName()))
                .thenComparing(table -> textForSort(table.tableName())));
        int total = tables.size();
        int fromIndex = Math.min(offset, total);
        int toIndex = Math.min(fromIndex + pageSize, total);
        List<DatabaseTableInfo> page = new ArrayList<>(tables.subList(fromIndex, toIndex));

        DatabaseMetadataScanResult result = baseScanResult(req, scanId, pageSize);
        result.setEstimatedTableCount(total);
        result.setTables(page);
        result.setCursor(toIndex < total ? String.valueOf(toIndex) : null);
        result.setProgress(scanProgress(toIndex, total, pageSize, toIndex < total));
        result.setPartialSummary(scanSummary(page.size(), total, req));
        result.setMetadataCache(scanMetadataCache(req, page));
        result.setResumeCommand(buildScanResumeCommand(req, result));
        result.getNextActions().add(toIndex < total
                ? "可使用 cursor 继续加载下一批表，或选择当前批次生成部分 metadata browser。"
                : "已到最后一批，可选择表生成 metadata browser 或反向导入预览。");
        return result;
    }

    private DatabaseMetadataCacheInfo scanMetadataCache(DatabaseMetadataScanReq req, List<DatabaseTableInfo> page) {
        List<String> tableNames = scanCacheTableNames(req, page);
        if (metadataCacheMode(req) == DatabaseMetadataCacheMode.REFRESH && !tableNames.isEmpty()) {
            DatabaseConnectionReq refreshReq = copyConnectionReq(req, tableNames);
            return metadataCacheService.resolveDump(refreshReq, () -> readDatabaseDump(refreshReq)).getMetadataCache();
        }
        return metadataCacheService.summarize(req, tableNames);
    }

    private List<String> scanCacheTableNames(DatabaseMetadataScanReq req, List<DatabaseTableInfo> page) {
        if (req.getTableNames() != null && !req.getTableNames().isEmpty()) {
            return req.getTableNames();
        }
        return page.stream()
                .map(DatabaseTableInfo::tableName)
                .toList();
    }

    private DatabaseConnectionReq copyConnectionReq(DatabaseConnectionReq source, List<String> tableNames) {
        DatabaseConnectionReq req = new DatabaseConnectionReq();
        req.setProjectId(source.getProjectId());
        req.setPresetId(source.getPresetId());
        req.setDatabaseType(source.getDatabaseType());
        req.setHost(source.getHost());
        req.setPort(source.getPort());
        req.setDatabaseName(source.getDatabaseName());
        req.setSchemaName(source.getSchemaName());
        req.setUsername(source.getUsername());
        req.setPassword(source.getPassword());
        req.setMetadataCacheMode(source.getMetadataCacheMode());
        req.setTableNames(new ArrayList<>(tableNames));
        return req;
    }

    private DatabaseMetadataCacheMode metadataCacheMode(DatabaseConnectionReq req) {
        if (req == null || req.getMetadataCacheMode() == null || req.getMetadataCacheMode().isBlank()) {
            return DatabaseMetadataCacheMode.AUTO;
        }
        try {
            return DatabaseMetadataCacheMode.valueOf(req.getMetadataCacheMode().trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return DatabaseMetadataCacheMode.AUTO;
        }
    }

    @Override
    public DatabaseSchemaDump exportDump(DatabaseConnectionReq req) {
        validateConnectionReq(req);
        if (req.getTableNames() == null || req.getTableNames().isEmpty()) {
            throw new BizException("请至少选择一张表");
        }
        return sanitizeDump(metadataCacheService.resolveDump(req, () -> readDatabaseDump(req)), req);
    }

    private DatabaseSchemaDump readDatabaseDump(DatabaseConnectionReq req) {
        try (Connection connection = connectionProvider.open(req)) {
            return sanitizeDump(metadataAdapter.exportDump(connection, req), req);
        } catch (SQLException e) {
            throw new BizException("读取数据库表结构失败: " + sanitizeConnectionError(e.getMessage(), req));
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
        preview.setMetadataCache(dump.getMetadataCache());
        return preview;
    }

    @Override
    public ReverseImportPreview previewDump(DatabaseSchemaDumpReq req) {
        ReverseImportPreview preview = reverseImportService.previewTables(req.getProjectId(), metadataAdapter.toTableDefs(req.getProjectId(), req.getDump()));
        preview.setMetadataCache(req.getDump().getMetadataCache());
        return preview;
    }

    @Override
    public ReverseImportCompareResult compare(DatabaseConnectionReq req) {
        DatabaseSchemaDump dump = exportDump(req);
        ReverseImportCompareResult result = reverseImportService.compareTables(req.getProjectId(), metadataAdapter.toTableDefs(req.getProjectId(), dump));
        result.setMetadataCache(dump.getMetadataCache());
        return result;
    }

    @Override
    public ReverseImportCompareResult compareDump(DatabaseSchemaDumpReq req) {
        ReverseImportCompareResult result = reverseImportService.compareTables(req.getProjectId(), metadataAdapter.toTableDefs(req.getProjectId(), req.getDump()));
        result.setMetadataCache(req.getDump().getMetadataCache());
        return result;
    }

    @Override
    public DatabaseSchemaChangePlan planSchemaChange(DatabaseConnectionReq req) {
        DatabaseSchemaDump dump = exportDump(req);
        DatabaseSchemaDumpReq dumpReq = new DatabaseSchemaDumpReq();
        dumpReq.setProjectId(req.getProjectId());
        dumpReq.setDump(dump);
        ReverseImportCompareResult compare = compareDump(dumpReq);

        DatabaseSchemaChangePlan plan = new DatabaseSchemaChangePlan();
        plan.setProjectId(req.getProjectId());
        plan.setDatabaseType(sanitizeMetadataText(normalizeDiagnosticType(databaseType(req)), req));
        plan.setDatabaseName(sanitizeMetadataText(dump.getDatabaseName(), req));
        plan.setSchemaName(sanitizeMetadataText(dump.getSchemaName(), req));
        plan.setCurrentSchemaHash(sha256Hex(schemaHashSource(dump)));
        plan.setTargetSpecHash(sha256Hex(targetHashSource(compare)));
        plan.setMetadataCache(dump.getMetadataCache());

        List<String> sqlDrafts = new ArrayList<>();
        for (ReverseImportTableDiff tableDiff : compare.getTableDiffs()) {
            for (ReverseImportFieldDiff diff : tableDiff.getFieldDiffs()) {
                plan.getChangeSet().addAll(changeItems(diff, req, sqlDrafts));
            }
        }
        plan.setSummary(schemaPlanSummary(dump, plan.getChangeSet()));
        plan.setRiskLevel(overallSchemaPlanRisk(plan.getSummary()));
        plan.setMigrationSql(sanitizeMetadataText(String.join("\n\n", sqlDrafts), req));
        plan.setRollbackHint(schemaPlanRollbackHint(plan.getRiskLevel()));
        plan.setManualChecks(schemaPlanManualChecks(plan.getChangeSet(), req));
        plan.setBlockedReasons(schemaPlanBlockedReasons(plan.getChangeSet(), req));
        plan.setNextActions(schemaPlanNextActions(plan));
        if (isBlank(plan.getMigrationSql())) {
            plan.setMigrationSql("-- DataSpec schema plan: no schema changes suggested for the selected metadata scope.");
        }
        return plan;
    }

    @Override
    public FieldCoverageReport coverage(DatabaseConnectionReq req) {
        if (fieldCoverageService == null) {
            throw new BizException("字段覆盖率服务未初始化");
        }
        DatabaseSchemaDump dump = exportDump(req);
        FieldCoverageReport report = fieldCoverageService.reportTables(req.getProjectId(), metadataAdapter.toTableDefs(req.getProjectId(), dump));
        report.setMetadataCache(dump.getMetadataCache());
        return report;
    }

    @Override
    public FieldCoverageReport coverageDump(DatabaseSchemaDumpReq req) {
        if (fieldCoverageService == null) {
            throw new BizException("字段覆盖率服务未初始化");
        }
        FieldCoverageReport report = fieldCoverageService.reportTables(req.getProjectId(), metadataAdapter.toTableDefs(req.getProjectId(), req.getDump()));
        report.setMetadataCache(req.getDump().getMetadataCache());
        return report;
    }

    private List<DatabaseSchemaChangeItem> changeItems(ReverseImportFieldDiff diff,
                                                        DatabaseConnectionReq req,
                                                        List<String> sqlDrafts) {
        if (diff == null || diff.getStatus() == null || ReverseImportFieldStatus.MATCHED.equals(diff.getStatus())) {
            return List.of();
        }
        if (ReverseImportFieldStatus.NEW.equals(diff.getStatus()) || Boolean.TRUE.equals(diff.getNonStandard())) {
            DatabaseSchemaChangeItem item = baseSchemaChangeItem(diff, req);
            item.setAction(DatabaseSchemaChangeAction.DROP_CANDIDATE);
            item.setProperty("column");
            item.setCurrentValue(sanitizeMetadataText(diff.getDataType(), req));
            item.setRiskLevel(SCHEMA_RISK_HIGH);
            item.setReason("字段未命中 DataSpec 标准，第一版只作为删除候选提示，不生成可执行 DROP。");
            String blocked = "未纳管字段 " + diff.getTableName() + "." + diff.getColumnName()
                    + " 需要人工确认是否补标准、保留兼容或另行迁移，不能自动删除。";
            item.getBlockedReasons().add(sanitizeMetadataText(blocked, req));
            item.getManualChecks().add(sanitizeMetadataText("确认 " + diff.getTableName() + "." + diff.getColumnName()
                    + " 是否仍被业务代码、报表或历史数据依赖。", req));
            item.setMigrationSql(sanitizeMetadataText("-- BLOCKED DROP_CANDIDATE "
                    + qualifiedColumn(diff, req) + ": review manually before writing destructive SQL.", req));
            item.setRollbackHint("未生成可执行删除 SQL；如后续人工删除，必须先准备备份和反向 ADD COLUMN 迁移。");
            sqlDrafts.add(item.getMigrationSql());
            return List.of(item);
        }

        List<DatabaseSchemaChangeItem> items = new ArrayList<>();
        for (ReverseImportFieldChange change : diff.getChanges()) {
            DatabaseSchemaChangeItem item = changeItem(diff, change, req);
            if (item != null) {
                items.add(item);
                sqlDrafts.add(item.getMigrationSql());
            }
        }
        return items;
    }

    private DatabaseSchemaChangeItem changeItem(ReverseImportFieldDiff diff,
                                                ReverseImportFieldChange change,
                                                DatabaseConnectionReq req) {
        if (change == null || isBlank(change.getProperty())) {
            return null;
        }
        DatabaseSchemaChangeItem item = baseSchemaChangeItem(diff, req);
        item.setProperty(change.getProperty());
        item.setCurrentValue(sanitizeMetadataText(change.getCurrentValue(), req));
        item.setTargetValue(sanitizeMetadataText(change.getStandardValue(), req));
        if ("comment".equals(change.getProperty())) {
            item.setAction(DatabaseSchemaChangeAction.ALTER_COMMENT);
            item.setRiskLevel(SCHEMA_RISK_LOW);
            item.setReason("字段注释与 DataSpec 标准不一致，可生成注释修正草案。");
            item.setMigrationSql(sanitizeMetadataText(commentSql(diff, change.getStandardValue(), req), req));
            item.setRollbackHint("回滚时可将字段注释恢复为当前值：" + nullToDash(change.getCurrentValue()));
            return item;
        }

        item.setAction(DatabaseSchemaChangeAction.ALTER_COLUMN);
        item.setRiskLevel(SCHEMA_RISK_MEDIUM);
        item.setReason("字段结构属性与 DataSpec 标准不一致，执行前需要确认数据兼容性。");
        item.getManualChecks().add(sanitizeMetadataText("确认 " + diff.getTableName() + "." + diff.getColumnName()
                + " 的 " + change.getProperty() + " 变更不会破坏历史数据、索引、默认值或业务代码。", req));
        item.setMigrationSql(sanitizeMetadataText(alterColumnSql(diff, change, req), req));
        item.setRollbackHint("回滚时按当前值恢复 " + change.getProperty() + "=" + nullToDash(change.getCurrentValue()) + "。");
        return item;
    }

    private DatabaseSchemaChangeItem baseSchemaChangeItem(ReverseImportFieldDiff diff, DatabaseConnectionReq req) {
        DatabaseSchemaChangeItem item = new DatabaseSchemaChangeItem();
        item.setTableName(sanitizeMetadataText(diff.getTableName(), req));
        item.setColumnName(sanitizeMetadataText(diff.getColumnName(), req));
        item.setStandardFieldName(sanitizeMetadataText(diff.getStandardFieldName(), req));
        return item;
    }

    private DatabaseSchemaChangeSummary schemaPlanSummary(DatabaseSchemaDump dump, List<DatabaseSchemaChangeItem> items) {
        DatabaseSchemaChangeSummary summary = new DatabaseSchemaChangeSummary();
        summary.setTableCount(dump.getTables().size());
        summary.setColumnCount(dump.getTables().stream().mapToInt(table -> table.getColumns().size()).sum());
        summary.setChangeCount(items.size());
        for (DatabaseSchemaChangeItem item : items) {
            if (SCHEMA_RISK_LOW.equals(item.getRiskLevel())) {
                summary.setLowRiskCount(summary.getLowRiskCount() + 1);
            } else if (SCHEMA_RISK_MEDIUM.equals(item.getRiskLevel())) {
                summary.setMediumRiskCount(summary.getMediumRiskCount() + 1);
            } else if (SCHEMA_RISK_HIGH.equals(item.getRiskLevel())) {
                summary.setHighRiskCount(summary.getHighRiskCount() + 1);
            }
            if (item.getBlockedReasons() != null && !item.getBlockedReasons().isEmpty()) {
                summary.setBlockedCount(summary.getBlockedCount() + 1);
            }
        }
        return summary;
    }

    private String overallSchemaPlanRisk(DatabaseSchemaChangeSummary summary) {
        if (summary.getBlockedCount() > 0) {
            return SCHEMA_RISK_BLOCKED;
        }
        if (summary.getHighRiskCount() > 0) {
            return SCHEMA_RISK_HIGH;
        }
        if (summary.getMediumRiskCount() > 0) {
            return SCHEMA_RISK_MEDIUM;
        }
        if (summary.getLowRiskCount() > 0) {
            return SCHEMA_RISK_LOW;
        }
        return RISK_SAFE;
    }

    private List<String> schemaPlanManualChecks(List<DatabaseSchemaChangeItem> items, DatabaseConnectionReq req) {
        List<String> checks = new ArrayList<>();
        for (DatabaseSchemaChangeItem item : items) {
            for (String check : item.getManualChecks()) {
                addDistinct(checks, sanitizeMetadataText(check, req));
            }
        }
        if (!items.isEmpty()) {
            addDistinct(checks, "执行正式迁移前，先把 dry-run SQL 交给迁移工具或 DBA 审阅，并在备份/恢复方案可用后再 apply。");
        }
        return checks;
    }

    private List<String> schemaPlanBlockedReasons(List<DatabaseSchemaChangeItem> items, DatabaseConnectionReq req) {
        List<String> reasons = new ArrayList<>();
        for (DatabaseSchemaChangeItem item : items) {
            for (String reason : item.getBlockedReasons()) {
                addDistinct(reasons, sanitizeMetadataText(reason, req));
            }
        }
        return reasons;
    }

    private List<String> schemaPlanNextActions(DatabaseSchemaChangePlan plan) {
        List<String> actions = new ArrayList<>();
        if (RISK_SAFE.equals(plan.getRiskLevel())) {
            actions.add("当前选择范围没有需要生成迁移草案的字段差异。");
            return actions;
        }
        actions.add("先审阅 changeSet、manualChecks 和 blockedReasons，再决定是否生成正式迁移文件。");
        if (!plan.getBlockedReasons().isEmpty()) {
            actions.add("高风险或阻塞项需要人工确认后再交给迁移工具。");
        }
        actions.add("正式迁移前建议导出项目备份，并在业务仓库中新增可回滚迁移脚本。");
        return actions;
    }

    private String schemaPlanRollbackHint(String riskLevel) {
        if (SCHEMA_RISK_BLOCKED.equals(riskLevel) || SCHEMA_RISK_HIGH.equals(riskLevel)) {
            return "存在高风险或阻塞项，未生成可自动执行计划；必须先确认备份、回滚 SQL 和业务代码引用。";
        }
        if (SCHEMA_RISK_MEDIUM.equals(riskLevel)) {
            return "结构属性变更需要按字段当前值准备反向 ALTER，并在测试库验证数据兼容。";
        }
        if (SCHEMA_RISK_LOW.equals(riskLevel)) {
            return "低风险注释变更可通过恢复原注释回滚。";
        }
        return "无迁移草案，无需回滚。";
    }

    private String commentSql(ReverseImportFieldDiff diff, String targetComment, DatabaseConnectionReq req) {
        if (TYPE_MYSQL.equals(databaseType(req))) {
            return "-- REVIEW MySQL comment change for " + qualifiedColumn(diff, req)
                    + ": generate MODIFY COLUMN with the full column definition before execution.";
        }
        return "COMMENT ON COLUMN " + qualifiedColumn(diff, req) + " IS " + sqlLiteral(targetComment) + ";";
    }

    private String alterColumnSql(ReverseImportFieldDiff diff, ReverseImportFieldChange change, DatabaseConnectionReq req) {
        String column = qualifiedColumn(diff, req);
        if (TYPE_MYSQL.equals(databaseType(req))) {
            return "-- REVIEW MySQL " + change.getProperty() + " change for " + column
                    + ": generate ALTER TABLE MODIFY COLUMN with full type/nullability/default context.";
        }
        // 结构属性可能包含 AI/用户维护的类型或默认值片段，第一版只写入 REVIEW 注释，避免复制即执行的 SQL 注入或破坏性 DROP。
        return "-- REVIEW PostgreSQL " + change.getProperty() + " change for " + column
                + ": confirm data compatibility and generate a reviewed migration with DBA/tooling.";
    }

    private String qualifiedColumn(ReverseImportFieldDiff diff, DatabaseConnectionReq req) {
        String schemaName = sqlSchemaName(req);
        if (!isBlank(schemaName)) {
            return quoteIdentifier(schemaName, req) + "." + quoteIdentifier(diff.getTableName(), req)
                    + "." + quoteIdentifier(diff.getColumnName(), req);
        }
        return quoteIdentifier(diff.getTableName(), req) + "." + quoteIdentifier(diff.getColumnName(), req);
    }

    private String sqlSchemaName(DatabaseConnectionReq req) {
        if (TYPE_MYSQL.equals(databaseType(req))) {
            return null;
        }
        return firstNonBlank(req.getSchemaName(), "public");
    }

    private String quoteIdentifier(String value, DatabaseConnectionReq req) {
        String quote = TYPE_MYSQL.equals(databaseType(req)) ? "`" : "\"";
        String escaped = singleLineSqlIdentifier(value).replace(quote, quote + quote);
        return quote + escaped + quote;
    }

    private String singleLineSqlIdentifier(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder safe = new StringBuilder(value.length());
        boolean lastWasSpace = false;
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (Character.isISOControl(ch)) {
                if (!lastWasSpace) {
                    safe.append(' ');
                    lastWasSpace = true;
                }
                continue;
            }
            safe.append(ch);
            lastWasSpace = Character.isWhitespace(ch);
        }
        return safe.toString().trim();
    }

    private String schemaHashSource(DatabaseSchemaDump dump) {
        StringBuilder source = new StringBuilder();
        source.append(dump.getDatabaseType()).append('|')
                .append(dump.getDatabaseName()).append('|')
                .append(dump.getSchemaName()).append('|');
        for (DatabaseSchemaTable table : dump.getTables()) {
            source.append("table:")
                    .append(table.getSchemaName()).append('.')
                    .append(table.getTableName()).append(':')
                    .append(table.getTableType()).append(':')
                    .append(table.getComment()).append('|');
            for (DatabaseSchemaColumn column : table.getColumns()) {
                source.append("column:")
                        .append(column.getOrdinalPosition()).append(':')
                        .append(column.getColumnName()).append(':')
                        .append(column.getDataType()).append(':')
                        .append(column.getNullable()).append(':')
                        .append(column.getDefaultValue()).append(':')
                        .append(column.getComment()).append('|');
            }
            for (DatabaseSchemaIndex index : table.getIndexes()) {
                source.append("index:")
                        .append(index.getIndexName()).append(':')
                        .append(index.getColumnName()).append(':')
                        .append(index.getNonUnique()).append('|');
            }
        }
        return source.toString();
    }

    private String targetHashSource(ReverseImportCompareResult compare) {
        StringBuilder source = new StringBuilder();
        for (ReverseImportTableDiff table : compare.getTableDiffs()) {
            for (ReverseImportFieldDiff diff : table.getFieldDiffs()) {
                source.append(diff.getTableName()).append('.')
                        .append(diff.getColumnName()).append(':')
                        .append(diff.getStandardFieldName()).append(':')
                        .append(diff.getStandardDisplayName()).append(':')
                        .append(diff.getStatus()).append('|');
                for (ReverseImportFieldChange change : diff.getChanges()) {
                    source.append(change.getProperty()).append('=')
                            .append(change.getStandardValue()).append('|');
                }
            }
        }
        return source.toString();
    }

    private void addDistinct(List<String> values, String value) {
        if (!isBlank(value) && !values.contains(value)) {
            values.add(value);
        }
    }

    private String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(nullToDash(value).getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm is required", e);
        }
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
        browser.setMetadataCache(dump.getMetadataCache());
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
        if (browser.getMetadataCache() != null) {
            text.append("metadataFingerprint=").append(nullToDash(browser.getMetadataCache().getMetadataFingerprint())).append('\n');
            text.append("metadataCache=")
                    .append(browser.getMetadataCache().getRefreshMode())
                    .append(" hit=").append(browser.getMetadataCache().isCacheHit())
                    .append(" stale=").append(browser.getMetadataCache().isStale())
                    .append('\n');
        }
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

    private DatabaseMetadataScanResult baseScanResult(DatabaseMetadataScanReq req, String scanId, int pageSize) {
        DatabaseMetadataScanResult result = new DatabaseMetadataScanResult();
        result.setProjectId(req.getProjectId());
        result.setDatabaseType(sanitizeMetadataText(databaseType(req).toUpperCase(Locale.ROOT), req));
        result.setDatabaseName(sanitizeMetadataText(req.getDatabaseName(), req));
        result.setSchemaName(sanitizeMetadataText(req.getSchemaName(), req));
        result.setScanId(scanId);
        result.setProgress(scanProgress(0, 0, pageSize, false));
        result.setPartialSummary(scanSummary(0, 0, req));
        return result;
    }

    private DatabaseMetadataScanResult cancelledScanResult(DatabaseMetadataScanReq req,
                                                           String scanId,
                                                           int pageSize,
                                                           int processedCount) {
        DatabaseMetadataScanResult result = baseScanResult(req, scanId, pageSize);
        result.setCancelled(true);
        result.setCursor(null);
        result.setProgress(scanProgress(processedCount, processedCount, pageSize, false));
        result.setResumeCommand(buildScanResumeCommand(req, result));
        result.getNextActions().add("扫描已取消；不会继续读取后续批次，也不会写入源库或标准字段库。");
        return result;
    }

    private DatabaseMetadataScanProgress scanProgress(int processedCount,
                                                       int estimatedTotal,
                                                       int pageSize,
                                                       boolean hasMore) {
        DatabaseMetadataScanProgress progress = new DatabaseMetadataScanProgress();
        progress.setProcessedTableCount(processedCount);
        progress.setRemainingTableEstimate(Math.max(0, estimatedTotal - processedCount));
        progress.setPageSize(pageSize);
        progress.setHasMore(hasMore);
        return progress;
    }

    private DatabaseMetadataScanSummary scanSummary(int pageTableCount, int estimatedTotal, DatabaseMetadataScanReq req) {
        DatabaseMetadataScanSummary summary = new DatabaseMetadataScanSummary();
        summary.setPageTableCount(pageTableCount);
        summary.setSelectedTableCount(req.getTableNames() == null ? 0 : req.getTableNames().size());
        summary.setEstimatedTableCount(estimatedTotal);
        return summary;
    }

    private String buildScanResumeCommand(DatabaseMetadataScanReq req, DatabaseMetadataScanResult result) {
        String cursor = result.getCursor() == null ? "DONE" : result.getCursor();
        String command = "POST /api/reverse-import/database/scan"
                + " projectId=" + req.getProjectId()
                + " databaseType=" + databaseType(req)
                + " databaseName=" + req.getDatabaseName()
                + " schemaName=" + nullToDash(req.getSchemaName())
                + " scanId=" + result.getScanId()
                + " cursor=" + cursor
                + " pageSize=" + result.getProgress().getPageSize();
        if (result.getMetadataCache() != null && result.getMetadataCache().getMetadataFingerprint() != null) {
            command += " metadataFingerprint=" + result.getMetadataCache().getMetadataFingerprint();
        }
        return SensitiveDataSanitizer.redactText(command, 1000, req.getPassword());
    }

    private String scanId(DatabaseMetadataScanReq req) {
        return isBlank(req.getScanId())
                ? "scan-" + UUID.randomUUID()
                : sanitizeMetadataText(req.getScanId(), req);
    }

    private int scanPageSize(Integer value) {
        if (value == null) {
            return DEFAULT_SCAN_PAGE_SIZE;
        }
        if (value < 1) {
            throw new BizException("分页大小不能小于 1");
        }
        return Math.min(value, MAX_SCAN_PAGE_SIZE);
    }

    private int scanOffset(String cursor) {
        if (isBlank(cursor)) {
            return 0;
        }
        try {
            int offset = Integer.parseInt(cursor.trim());
            if (offset < 0) {
                throw new BizException("扫描 cursor 不能为负数");
            }
            return offset;
        } catch (NumberFormatException e) {
            throw new BizException("扫描 cursor 格式不正确");
        }
    }

    private String textForSort(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private String nullToDash(String value) {
        return isBlank(value) ? "-" : value;
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
                readPostgresqlSignals(statement, signals, req);
            } else {
                readMysqlSignals(statement, signals);
            }
        } catch (SQLException e) {
            warnings.add("权限诊断查询失败: " + sanitizeConnectionError(e.getMessage(), req));
        }
        return signals;
    }

    private void readPostgresqlSignals(Statement statement,
                                       Map<String, Object> signals,
                                       DatabaseConnectionReq req) throws SQLException {
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
        String schema = schemaPattern(req);
        boolean canCreateSchema = queryBoolean(statement,
                "select has_schema_privilege(" + sqlLiteral(schema) + ", 'CREATE')");
        boolean canWriteTables = queryBoolean(statement, """
                select exists (
                    select 1
                    from information_schema.tables
                    where table_schema = %s
                      and table_type = 'BASE TABLE'
                      and (
                          has_table_privilege(format('%%I.%%I', table_schema, table_name), 'INSERT')
                          or has_table_privilege(format('%%I.%%I', table_schema, table_name), 'UPDATE')
                          or has_table_privilege(format('%%I.%%I', table_schema, table_name), 'DELETE')
                          or has_table_privilege(format('%%I.%%I', table_schema, table_name), 'TRUNCATE')
                          or has_table_privilege(format('%%I.%%I', table_schema, table_name), 'REFERENCES')
                          or has_table_privilege(format('%%I.%%I', table_schema, table_name), 'TRIGGER')
                      )
                )
                """.formatted(sqlLiteral(schema)));
        boolean writeRisk = canCreateDatabase || canCreateSchema || canWriteTables;
        signals.put("writeRisk", writeRisk);
        if (!writeRisk) {
            // PostgreSQL 的 JDBC/事务只读标志不等同于账号权限；无写权限时才可判定为只读安全账号。
            signals.put("readOnly", true);
        }
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

    private String sqlLiteral(String value) {
        return "'" + (value == null ? "" : value.replace("'", "''")) + "'";
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
