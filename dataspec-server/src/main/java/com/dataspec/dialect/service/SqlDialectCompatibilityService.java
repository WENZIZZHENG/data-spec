package com.dataspec.dialect.service;

import com.dataspec.dialect.model.DialectCapability;
import com.dataspec.dialect.model.DialectDiagnostic;
import com.dataspec.dialect.model.DialectSupportLevel;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * PostgreSQL/MySQL 方言兼容矩阵与运行时诊断。
 *
 * <p>第一版刻意使用确定性规则，不把未通过项目 fixture 覆盖的能力标成 SUPPORTED。
 * 后续如果接入更强 parser 或外部 linter，只需要在这里扩展诊断来源。</p>
 */
@Service
public class SqlDialectCompatibilityService {

    public static final String DIALECT_POSTGRESQL = "postgresql";
    public static final String DIALECT_MYSQL = "mysql";
    public static final String DIALECT_UNKNOWN = "unknown";

    private static final Pattern MYSQL_FEATURES = Pattern.compile(
            "(?is)(`[^`]+`|\\bAUTO_INCREMENT\\b|\\bUNSIGNED\\b|\\bTINYINT\\s*\\(\\s*1\\s*\\)|\\bKEY\\s+\\w+\\b|\\bENGINE\\s*=|\\bDEFAULT\\s+CHARSET\\b|\\bCHARSET\\s*=|\\bCOLLATE\\s*=|\\bCOMMENT\\s+'|\\bCOMMENT\\s*=)");
    private static final Pattern POSTGRESQL_FEATURES = Pattern.compile(
            "(?is)(\\bCOMMENT\\s+ON\\b|\\bBIGSERIAL\\b|\\bSMALLSERIAL\\b|\\bSERIAL\\b|\\bTIMESTAMPTZ\\b|\\bPUBLIC\\.)");
    private static final Pattern MYSQL_INDEX_OR_OPTIONS = Pattern.compile(
            "(?is)(\\bPRIMARY\\s+KEY\\b|\\bUNIQUE\\s+KEY\\b|\\bKEY\\s+`?\\w+`?\\b|\\bENGINE\\s*=|\\bDEFAULT\\s+CHARSET\\b|\\bCOLLATE\\s*=)");

    public String inferDialect(String sql) {
        String normalized = sql == null ? "" : sql;
        boolean mysql = MYSQL_FEATURES.matcher(normalized).find();
        boolean postgresql = POSTGRESQL_FEATURES.matcher(normalized).find();
        if (mysql && postgresql) {
            return DIALECT_UNKNOWN;
        }
        if (mysql) {
            return DIALECT_MYSQL;
        }
        return DIALECT_POSTGRESQL;
    }

    public List<DialectDiagnostic> diagnoseSql(String sql) {
        return diagnoseSql(sql, false);
    }

    public List<DialectDiagnostic> diagnoseSql(String sql, boolean fixedSqlGenerated) {
        String normalized = sql == null ? "" : sql;
        String dialect = inferDialect(normalized);
        List<DialectDiagnostic> diagnostics = new ArrayList<>();
        if (DIALECT_UNKNOWN.equals(dialect)) {
            diagnostics.add(diagnostic(
                    DIALECT_UNKNOWN,
                    DialectCapability.DIALECT_DETECTION,
                    DialectSupportLevel.WARNING,
                    "MIXED_DIALECT_HINT",
                    "SQL 同时包含 PostgreSQL 与 MySQL 特征，DataSpec 无法可靠判断唯一方言。",
                    "建议拆分 SQL 或在数据库直连/后续 dialect 参数中显式选择方言。"));
            return diagnostics;
        }

        if (DIALECT_MYSQL.equals(dialect)) {
            addMysqlSqlDiagnostics(normalized, diagnostics, fixedSqlGenerated);
            return diagnostics;
        }

        addPostgresqlSqlDiagnostics(normalized, diagnostics, fixedSqlGenerated);
        return diagnostics;
    }

    public List<DialectDiagnostic> diagnoseGeneratedPostgresqlDdl() {
        return List.of(
                diagnostic(
                        DIALECT_POSTGRESQL,
                        DialectCapability.DDL_GENERATION,
                        DialectSupportLevel.SUPPORTED,
                        "POSTGRESQL_DDL_TARGET",
                        "DDL 生成器当前输出 PostgreSQL 风格 CREATE TABLE 与 COMMENT ON 语句。",
                        "可直接交给 PostgreSQL 路径继续 lint；MySQL 需先做方言转换。"),
                diagnostic(
                        DIALECT_POSTGRESQL,
                        DialectCapability.TYPE_MAPPING,
                        DialectSupportLevel.PARTIAL,
                        "MYSQL_DDL_CONVERSION_REQUIRED",
                        "生成结果不承诺可直接在 MySQL 执行。",
                        "如目标库是 MySQL，请先用方言转换或人工检查类型、注释和自增语法。")
        );
    }

    public List<DialectDiagnostic> diagnoseDatabase(String databaseType, String schemaName) {
        String dialect = normalizeDialect(databaseType);
        if (!DIALECT_POSTGRESQL.equals(dialect) && !DIALECT_MYSQL.equals(dialect)) {
            return List.of(diagnostic(
                    DIALECT_UNKNOWN,
                    DialectCapability.DATABASE_METADATA,
                    DialectSupportLevel.UNSUPPORTED,
                    "UNSUPPORTED_DATABASE_DIALECT",
                    "数据库直连第一版仅验证 PostgreSQL 与 MySQL。",
                    "请改用 PostgreSQL/MySQL 连接，或先通过 SQL DDL 文本模式导入。"));
        }
        if (DIALECT_MYSQL.equals(dialect)) {
            return List.of(
                    diagnostic(
                            DIALECT_MYSQL,
                            DialectCapability.DATABASE_METADATA,
                            DialectSupportLevel.SUPPORTED,
                            "MYSQL_DATABASE_METADATA",
                            "数据库直连使用 MySQL JDBC metadata 与 information_schema remarks 读取表字段。",
                            "保持连接参数 useInformationSchema/remarks 可用，并优先选择明确表范围。"),
                    diagnostic(
                            DIALECT_MYSQL,
                            DialectCapability.SCHEMA_CATALOG,
                            DialectSupportLevel.PARTIAL,
                            "MYSQL_CATALOG_SCHEMA_BOUNDARY",
                            "MySQL databaseName 作为 catalog 使用，schemaName 不参与表过滤。",
                            "如同实例多库并存，请确认 databaseName 指向目标库。"));
        }
        String schema = schemaName == null || schemaName.isBlank() ? "public" : schemaName.trim();
        return List.of(
                diagnostic(
                        DIALECT_POSTGRESQL,
                        DialectCapability.DATABASE_METADATA,
                        DialectSupportLevel.SUPPORTED,
                        "POSTGRESQL_DATABASE_METADATA",
                        "数据库直连使用 PostgreSQL JDBC metadata 读取表字段与注释。",
                        "建议使用只读账号并限制 schema/table 范围。"),
                diagnostic(
                        DIALECT_POSTGRESQL,
                        DialectCapability.SCHEMA_CATALOG,
                        DialectSupportLevel.INFO,
                        "POSTGRESQL_SCHEMA_PATTERN",
                        "当前 PostgreSQL schema 过滤为 " + schema + "。",
                        "如未找到表，请确认 schemaName 是否正确。"));
    }

    public List<DialectDiagnostic> capabilityMatrix() {
        return List.of(
                diagnostic(DIALECT_POSTGRESQL, DialectCapability.COMMENTS, DialectSupportLevel.SUPPORTED,
                        "POSTGRESQL_COMMENT_ON_SUPPORTED", "已验证 COMMENT ON TABLE/COLUMN 解析与导出。", "保持 COMMENT ON 与 CREATE TABLE 同批输入。"),
                diagnostic(DIALECT_POSTGRESQL, DialectCapability.AUTO_INCREMENT, DialectSupportLevel.SUPPORTED,
                        "POSTGRESQL_SERIAL_SUPPORTED", "已验证 serial/bigserial 作为 PostgreSQL 自增类型路径。", "优先使用 bigserial 或 identity 迁移计划。"),
                diagnostic(DIALECT_MYSQL, DialectCapability.COMMENTS, DialectSupportLevel.PARTIAL,
                        "MYSQL_INLINE_COMMENT_PARTIAL", "已覆盖 MySQL inline COMMENT 常见解析，但 fixedSql 不保证 MySQL 原样输出。", "应用 fixedSql 前人工检查注释语法。"),
                diagnostic(DIALECT_MYSQL, DialectCapability.INDEXES_CONSTRAINTS, DialectSupportLevel.PARTIAL,
                        "MYSQL_INDEX_TABLE_OPTION_PARTIAL", "已避免把 MySQL KEY/ENGINE/CHARSET 当作字段，但不生成完整索引模型。", "索引治理保留在迁移计划或后续专项。")
        );
    }

    public String normalizeDialect(String databaseType) {
        if (databaseType == null || databaseType.isBlank()) {
            return DIALECT_UNKNOWN;
        }
        String normalized = databaseType.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "postgres", "postgresql" -> DIALECT_POSTGRESQL;
            case "mysql" -> DIALECT_MYSQL;
            default -> DIALECT_UNKNOWN;
        };
    }

    private void addPostgresqlSqlDiagnostics(String sql, List<DialectDiagnostic> diagnostics, boolean fixedSqlGenerated) {
        diagnostics.add(diagnostic(
                DIALECT_POSTGRESQL,
                DialectCapability.DIALECT_DETECTION,
                DialectSupportLevel.INFO,
                "POSTGRESQL_DIALECT_INFERRED",
                "未发现 MySQL 专属特征，按 PostgreSQL/DataSpec 默认路径处理。",
                "如业务 SQL 实际为 MySQL 或混合方言，请优先使用数据库直连或后续显式 dialect 参数。"));
        if (containsIgnoreCase(sql, "COMMENT ON")) {
            diagnostics.add(diagnostic(
                    DIALECT_POSTGRESQL,
                    DialectCapability.COMMENTS,
                    DialectSupportLevel.SUPPORTED,
                    "POSTGRESQL_COMMENT_ON_SUPPORTED",
                    "已验证 PostgreSQL COMMENT ON TABLE/COLUMN 解析、lint 与 fixedSql 路径。",
                    "继续保持 COMMENT ON 与 CREATE TABLE 一起提交。"));
        }
        if (fixedSqlGenerated) {
            diagnostics.add(diagnostic(
                    DIALECT_POSTGRESQL,
                    DialectCapability.FIXED_SQL,
                    DialectSupportLevel.INFO,
                    "POSTGRESQL_FIXED_SQL_TARGET",
                    "fixedSql 按 PostgreSQL/DataSpec 默认 SQL 风格重建。",
                    "应用前仍建议查看 diff，确认是否符合目标库。"));
        }
    }

    private void addMysqlSqlDiagnostics(String sql, List<DialectDiagnostic> diagnostics, boolean fixedSqlGenerated) {
        diagnostics.add(diagnostic(
                DIALECT_MYSQL,
                DialectCapability.DIALECT_DETECTION,
                DialectSupportLevel.INFO,
                "MYSQL_DIALECT_INFERRED",
                "检测到 MySQL DDL 特征，按 MySQL 兼容路径给出诊断。",
                "继续检查 parser/lint 输出，并保留人工复核索引、字符集和自增语法。"));
        if (containsIgnoreCase(sql, "AUTO_INCREMENT")) {
            diagnostics.add(diagnostic(
                    DIALECT_MYSQL,
                    DialectCapability.AUTO_INCREMENT,
                    DialectSupportLevel.PARTIAL,
                    "MYSQL_AUTO_INCREMENT_PARTIAL",
                    "MySQL AUTO_INCREMENT 可参与解析，但 DataSpec 不生成完整自增迁移模型。",
                    "执行迁移前确认主键、自增起始值和目标数据库语法。"));
        }
        if (containsIgnoreCase(sql, "UNSIGNED")) {
            diagnostics.add(diagnostic(
                    DIALECT_MYSQL,
                    DialectCapability.TYPE_MAPPING,
                    DialectSupportLevel.PARTIAL,
                    "MYSQL_UNSIGNED_TYPE_PARTIAL",
                    "MySQL UNSIGNED 类型修饰可保留在解析结果中，但与 PostgreSQL 类型没有一一映射。",
                    "跨方言生成或迁移时请确认数值范围和目标类型。"));
        }
        if (MYSQL_INDEX_OR_OPTIONS.matcher(sql).find()) {
            diagnostics.add(diagnostic(
                    DIALECT_MYSQL,
                    DialectCapability.INDEXES_CONSTRAINTS,
                    DialectSupportLevel.PARTIAL,
                    "MYSQL_INDEX_TABLE_OPTION_PARTIAL",
                    "MySQL KEY/ENGINE/CHARSET 等选项不会作为标准字段导入，但索引模型第一版不完整。",
                    "索引、charset、collation 请在迁移计划或人工 SQL review 中单独确认。"));
        }
        if (sql.contains("`")) {
            diagnostics.add(diagnostic(
                    DIALECT_MYSQL,
                    DialectCapability.QUOTED_IDENTIFIER,
                    DialectSupportLevel.PARTIAL,
                    "MYSQL_BACKTICK_IDENTIFIER_PARTIAL",
                    "反引号标识符可被兼容解析，但 DataSpec 标准字段仍优先使用未 quote 的 snake_case。",
                    "建议把标准字段名维护为 snake_case，避免依赖大小写敏感标识符。"));
        }
        if (containsIgnoreCase(sql, " COMMENT ") || containsIgnoreCase(sql, "COMMENT=")) {
            diagnostics.add(diagnostic(
                    DIALECT_MYSQL,
                    DialectCapability.COMMENTS,
                    DialectSupportLevel.PARTIAL,
                    "MYSQL_INLINE_COMMENT_PARTIAL",
                    "MySQL inline COMMENT 可读取到字段/表注释，但 fixedSql 不保证输出 MySQL inline COMMENT。",
                    "应用 fixedSql 前人工检查注释语法，必要时保留 MySQL 原始 DDL。"));
        }
        if (fixedSqlGenerated) {
            diagnostics.add(diagnostic(
                    DIALECT_MYSQL,
                    DialectCapability.FIXED_SQL,
                    DialectSupportLevel.WARNING,
                    "MYSQL_FIXED_SQL_REVIEW_REQUIRED",
                    "fixedSql 第一版可能按 PostgreSQL/DataSpec 风格重建 COMMENT 或类型片段。",
                    "不要直接覆盖 MySQL 迁移文件；先看 diff，再手工转换 COMMENT、自增和索引语法。"));
        }
    }

    private DialectDiagnostic diagnostic(
            String dialect,
            DialectCapability capability,
            DialectSupportLevel level,
            String code,
            String message,
            String nextAction
    ) {
        return DialectDiagnostic.of(dialect, capability, level, code, message, nextAction);
    }

    private boolean containsIgnoreCase(String value, String needle) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(needle.toLowerCase(Locale.ROOT));
    }
}
