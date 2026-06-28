package com.dataspec.reverseimport.service.impl;

import com.dataspec.common.exception.BizException;
import com.dataspec.common.sanitize.SensitiveDataSanitizer;
import com.dataspec.coverage.model.FieldCoverageReport;
import com.dataspec.coverage.service.FieldCoverageService;
import com.dataspec.dialect.service.SqlDialectCompatibilityService;
import com.dataspec.reverseimport.model.DatabaseConnectionReq;
import com.dataspec.reverseimport.model.DatabaseConnectionResult;
import com.dataspec.reverseimport.model.DatabaseConnectionSecurityDiagnostic;
import com.dataspec.reverseimport.model.DatabaseSchemaDump;
import com.dataspec.reverseimport.model.DatabaseSchemaDumpReq;
import com.dataspec.reverseimport.model.DatabaseTableInfo;
import com.dataspec.reverseimport.model.ReverseImportCompareResult;
import com.dataspec.reverseimport.model.ReverseImportPreview;
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
        try (Connection connection = connectionProvider.open(req)) {
            return new DatabaseConnectionResult(true, "连接成功", diagnoseConnectionSecurity(connection, req));
        } catch (SQLException | RuntimeException e) {
            return new DatabaseConnectionResult(false, "连接失败: " + sanitizeConnectionError(e.getMessage(), req));
        }
    }

    @Override
    public List<DatabaseTableInfo> listTables(DatabaseConnectionReq req) {
        validateConnectionReq(req);
        try (Connection connection = connectionProvider.open(req)) {
            return metadataAdapter.listTables(connection, req);
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
            return metadataAdapter.exportDump(connection, req);
        } catch (SQLException e) {
            throw new BizException("读取数据库表结构失败: " + e.getMessage());
        }
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
