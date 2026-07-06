package com.dataspec.reverseimport;

import com.dataspec.coverage.model.FieldCoverageStatus;
import com.dataspec.coverage.service.impl.FieldCoverageServiceImpl;
import com.dataspec.field.entity.Field;
import com.dataspec.field.model.FieldSuggestion;
import com.dataspec.field.service.FieldService;
import com.dataspec.lint.engine.SqlParserService;
import com.dataspec.reverseimport.model.DatabaseConnectionReq;
import com.dataspec.reverseimport.model.DatabaseConnectionResult;
import com.dataspec.reverseimport.model.DatabaseConnectionSecurityDiagnostic;
import com.dataspec.reverseimport.model.DatabaseMetadataBrowser;
import com.dataspec.reverseimport.model.DatabaseSchemaColumn;
import com.dataspec.reverseimport.model.DatabaseSchemaDump;
import com.dataspec.reverseimport.model.DatabaseSchemaTable;
import com.dataspec.reverseimport.model.DatabaseTableInfo;
import com.dataspec.reverseimport.model.ReverseImportCompareResult;
import com.dataspec.reverseimport.model.ReverseImportFieldStatus;
import com.dataspec.reverseimport.service.ReverseImportSourceService;
import com.dataspec.reverseimport.service.impl.DatabaseReverseImportServiceImpl;
import com.dataspec.reverseimport.service.impl.JdbcDatabaseMetadataAdapter;
import com.dataspec.reverseimport.service.impl.ReverseImportServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * PostgreSQL/MySQL 真实 JDBC metadata 集成测试。
 *
 * <p>该测试只在 `db-integration` Maven profile 中运行。fixture 只创建 schema、空表、
 * 注释、索引和只读用户，用来验证真实驱动返回的 metadata，不依赖源库业务数据行。</p>
 */
@Testcontainers
class DatabaseMetadataTestcontainersIT {

    private static final long PROJECT_ID = 1L;
    private static final String TABLE_NAME = "user_order";
    private static final String POSTGRES_SCHEMA = "dataspec_it";
    private static final String READONLY_USERNAME = "dataspec_ro";
    private static final String POSTGRES_OWNER_PASSWORD = randomCredential();
    private static final String MYSQL_OWNER_PASSWORD = randomCredential();
    private static final String POSTGRES_READONLY_PASSWORD = randomCredential();
    private static final String MYSQL_READONLY_PASSWORD = randomCredential();

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
            DockerImageName.parse("postgres:16-alpine"))
            .withDatabaseName("dataspec_it")
            .withUsername("dataspec_owner")
            .withPassword(POSTGRES_OWNER_PASSWORD);

    @Container
    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>(
            DockerImageName.parse("mysql:8.0"))
            .withDatabaseName("dataspec_it")
            .withUsername("dataspec_owner")
            .withPassword(MYSQL_OWNER_PASSWORD);

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeAll
    static void initializeFixtures() throws Exception {
        initializePostgresqlFixture();
        initializeMysqlFixture();
    }

    @Test
    void postgresqlMatrix_readsMetadataThroughCoreWorkflows() throws Exception {
        assertDialectMetadataWorkflow(postgresqlFixture());
    }

    @Test
    void mysqlMatrix_readsMetadataThroughCoreWorkflows() throws Exception {
        DialectFixture fixture = mysqlFixture();

        assertDialectMetadataWorkflow(fixture);

        DatabaseSchemaDump dump = service(fieldService()).exportDump(fixture.request());
        assertThat(dump.getWarnings())
                .as("[%s] MySQL schemaName warning", fixture.label())
                .anyMatch(warning -> warning.contains("schemaName"));
    }

    @Test
    void readonlyDiagnostics_areSafeAndSecretSafe() throws Exception {
        assertReadonlyDiagnostic(postgresqlFixture());
        assertReadonlyDiagnostic(mysqlFixture());
    }

    private void assertDialectMetadataWorkflow(DialectFixture fixture) throws Exception {
        FieldService fieldService = fieldService();
        DatabaseReverseImportServiceImpl service = service(fieldService);
        DatabaseConnectionReq req = fixture.request();

        assertThat(service.listTables(req))
                .as("[%s] list table names", fixture.label())
                .extracting(DatabaseTableInfo::tableName)
                .anyMatch(TABLE_NAME::equalsIgnoreCase);

        DatabaseSchemaDump dump = service.exportDump(req);
        DatabaseSchemaTable table = onlyTable(dump, fixture);
        DatabaseSchemaColumn id = column(table, "id", fixture);
        DatabaseSchemaColumn phone = column(table, "phone", fixture);
        DatabaseSchemaColumn statusCode = column(table, "status_code", fixture);
        DatabaseSchemaColumn phoneAlias = column(table, "phone_alias", fixture);
        DatabaseSchemaColumn userName = column(table, "user_name", fixture);
        DatabaseSchemaColumn rawPayload = column(table, "raw_payload", fixture);

        assertThat(table.getComment()).as("[%s] table comment", fixture.label()).isEqualTo("用户订单");
        assertThat(id.getComment()).as("[%s] id comment", fixture.label()).isEqualTo("主键");
        assertThat(statusCode.getComment()).as("[%s] status_code comment", fixture.label()).isNullOrEmpty();
        assertThat(phoneAlias.getComment()).as("[%s] phone_alias comment", fixture.label()).isEqualTo("手机号");
        assertThat(userName.getComment()).as("[%s] user_name comment", fixture.label()).isEqualTo("用户名");
        assertThat(rawPayload.getComment()).as("[%s] raw_payload comment", fixture.label()).isEqualTo("扩展信息");
        assertThat(phone.getNullable()).as("[%s] phone nullable", fixture.label()).isTrue();
        assertThat(table.getIndexes()).as("[%s] index metadata", fixture.label())
                .anySatisfy(index -> {
                    assertThat(index.getIndexName()).as("[%s] index name", fixture.label())
                            .isEqualToIgnoringCase("idx_user_order_phone");
                    assertThat(index.getColumnName()).as("[%s] index column", fixture.label())
                            .isEqualToIgnoringCase("phone");
                });

        DatabaseMetadataBrowser browser = service.browse(req);
        assertThat(browser.getSummary().getTableCount()).as("[%s] browser table count", fixture.label()).isEqualTo(1);
        assertThat(browser.getSummary().getColumnCount()).as("[%s] browser column count", fixture.label()).isEqualTo(6);
        assertThat(browser.getSummary().getIndexCount()).as("[%s] browser index count", fixture.label()).isGreaterThanOrEqualTo(1);
        assertThat(browser.getPreview().getFieldCandidates())
                .as("[%s] preview candidates", fixture.label())
                .extracting(candidate -> normalize(candidate.getColumnName()))
                .contains("user_name", "raw_payload");

        ReverseImportCompareResult compare = service.compare(req);
        assertThat(compare.getSummary().getTableCount()).as("[%s] compare table count", fixture.label()).isEqualTo(1);
        assertThat(compare.getSummary().getColumnCount()).as("[%s] compare column count", fixture.label()).isEqualTo(6);
        assertThat(compare.getSummary().getMatchedCount()).as("[%s] compare matched count", fixture.label()).isEqualTo(4);
        assertThat(compare.getSummary().getChangedCount()).as("[%s] compare changed count", fixture.label()).isGreaterThanOrEqualTo(1);
        assertThat(compare.getSummary().getNewCount()).as("[%s] compare new count", fixture.label()).isEqualTo(2);
        assertThat(compare.getSummary().getMissingCommentCount()).as("[%s] compare missing comments", fixture.label()).isEqualTo(2);
        assertThat(compare.getSummary().getNonStandardCount()).as("[%s] compare non-standard count", fixture.label()).isEqualTo(2);
        assertThat(compare.getTableDiffs().get(0).getFieldDiffs())
                .as("[%s] compare field statuses", fixture.label())
                .extracting(diff -> normalize(diff.getColumnName()), diff -> diff.getStatus())
                .contains(
                        org.assertj.core.groups.Tuple.tuple("phone", ReverseImportFieldStatus.CHANGED),
                        org.assertj.core.groups.Tuple.tuple("status_code", ReverseImportFieldStatus.MISSING_COMMENT),
                        org.assertj.core.groups.Tuple.tuple("phone_alias", ReverseImportFieldStatus.MATCHED),
                        org.assertj.core.groups.Tuple.tuple("user_name", ReverseImportFieldStatus.NEW),
                        org.assertj.core.groups.Tuple.tuple("raw_payload", ReverseImportFieldStatus.NEW)
                );

        var coverage = service.coverage(req);
        assertThat(coverage.getSummary().getTableCount()).as("[%s] coverage table count", fixture.label()).isEqualTo(1);
        assertThat(coverage.getSummary().getColumnCount()).as("[%s] coverage column count", fixture.label()).isEqualTo(6);
        assertThat(coverage.getSummary().getCoveredCount()).as("[%s] coverage covered count", fixture.label()).isEqualTo(4);
        assertThat(coverage.getSummary().getUnmanagedCount()).as("[%s] coverage unmanaged count", fixture.label()).isEqualTo(2);
        assertThat(coverage.getSummary().getMissingCommentCount()).as("[%s] coverage missing comments", fixture.label()).isEqualTo(2);
        assertThat(coverage.getSummary().getPossibleDuplicateCount()).as("[%s] coverage duplicate count", fixture.label()).isEqualTo(1);
        assertThat(coverage.getSummary().getCoverageRate()).as("[%s] coverage rate", fixture.label()).isEqualTo(66.7);
        assertThat(coverage.getTables().get(0).getFields())
                .as("[%s] coverage field statuses", fixture.label())
                .extracting(item -> normalize(item.getColumnName()), item -> item.getStatus())
                .contains(
                        org.assertj.core.groups.Tuple.tuple("id", FieldCoverageStatus.STANDARD_MATCH),
                        org.assertj.core.groups.Tuple.tuple("phone", FieldCoverageStatus.MISSING_COMMENT),
                        org.assertj.core.groups.Tuple.tuple("status_code", FieldCoverageStatus.MISSING_COMMENT),
                        org.assertj.core.groups.Tuple.tuple("phone_alias", FieldCoverageStatus.ALIAS_MATCH),
                        org.assertj.core.groups.Tuple.tuple("user_name", FieldCoverageStatus.POSSIBLE_DUPLICATE),
                        org.assertj.core.groups.Tuple.tuple("raw_payload", FieldCoverageStatus.UNMANAGED)
                );

        String artifacts = objectMapper.writeValueAsString(List.of(dump, browser, compare, coverage));
        assertThat(artifacts).as("[%s] generated artifacts keep secrets out", fixture.label())
                .doesNotContain(fixture.password(), "jdbc:", "Bearer ");
        verify(fieldService, never()).create(org.mockito.ArgumentMatchers.any(Field.class));
    }

    private void assertReadonlyDiagnostic(DialectFixture fixture) throws Exception {
        DatabaseConnectionResult result = service(fieldService()).testConnection(fixture.request());

        assertThat(result.success()).as("[%s] connection success", fixture.label()).isTrue();
        assertThat(result.health()).as("[%s] health diagnostic", fixture.label()).isNotNull();
        assertThat(result.health().connectionStatus()).as("[%s] health status", fixture.label()).isEqualTo("CONNECTED");
        assertThat(result.health().dialect()).as("[%s] health dialect", fixture.label())
                .isEqualTo(fixture.databaseType().toUpperCase(Locale.ROOT));
        assertThat(result.health().capability().metadataReadable()).as("[%s] metadata readable", fixture.label()).isTrue();

        DatabaseConnectionSecurityDiagnostic security = result.security();
        assertThat(security).as("[%s] security diagnostic", fixture.label()).isNotNull();
        assertThat(security.currentUser()).as("[%s] current user", fixture.label()).contains(READONLY_USERNAME);
        assertThat(security.readOnly()).as("[%s] read-only indicator", fixture.label()).isTrue();
        assertThat(security.writeRisk()).as("[%s] write risk", fixture.label()).isFalse();
        assertThat(security.riskLevel()).as("[%s] risk level", fixture.label()).isEqualTo("SAFE");
        assertThat(security.accessibleTableCount()).as("[%s] readable table count", fixture.label()).isGreaterThanOrEqualTo(1);
        assertThat(security.warnings()).as("[%s] security warnings", fixture.label()).isEmpty();

        String diagnostic = objectMapper.writeValueAsString(result);
        assertThat(diagnostic).as("[%s] diagnostic keeps secrets out", fixture.label())
                .doesNotContain(fixture.password(), "jdbc:", "Bearer ");
    }

    private DatabaseReverseImportServiceImpl service(FieldService fieldService) {
        ReverseImportServiceImpl reverseImportService = new ReverseImportServiceImpl(
                new SqlParserService(),
                fieldService,
                mock(ReverseImportSourceService.class));
        return new DatabaseReverseImportServiceImpl(
                reverseImportService,
                new FieldCoverageServiceImpl(fieldService, new SqlParserService()),
                new JdbcDatabaseMetadataAdapter(),
                null);
    }

    private FieldService fieldService() {
        Field id = standardField(1L, "id", "主键", "BIGINT", null, false, "主键", null);
        Field mobileNo = standardField(2L, "mobile_no", "手机号", "VARCHAR", 30, true, "手机号", "phone,phone_alias");
        Field statusCode = standardField(3L, "status_code", "状态编码", "VARCHAR", 20, true, "状态编码", null);
        Field accountName = standardField(4L, "account_name", "账号名称", "VARCHAR", 50, true, "账号名称", null);
        FieldService fieldService = mock(FieldService.class);
        when(fieldService.listByProject(PROJECT_ID)).thenReturn(List.of(id, mobileNo, statusCode));
        when(fieldService.suggest(eq(PROJECT_ID), anyString(), eq(1))).thenAnswer(invocation -> {
            String query = invocation.getArgument(1, String.class);
            if ("user_name".equalsIgnoreCase(query)) {
                return List.of(new FieldSuggestion(accountName, 88, "真实库字段疑似账号名称", "account_name", true));
            }
            return List.of();
        });
        return fieldService;
    }

    private Field standardField(Long id,
                                String name,
                                String displayName,
                                String dataType,
                                Integer length,
                                Boolean nullable,
                                String comment,
                                String aliases) {
        Field field = new Field();
        field.setId(id);
        field.setProjectId(PROJECT_ID);
        field.setName(name);
        field.setDisplayName(displayName);
        field.setDataType(dataType);
        field.setLength(length);
        field.setNullable(nullable);
        field.setComment(comment);
        field.setAliases(aliases);
        field.setStatus("enabled");
        return field;
    }

    private static void initializePostgresqlFixture() throws SQLException {
        try (Connection connection = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(),
                POSTGRES.getUsername(),
                POSTGRES.getPassword());
             Statement statement = connection.createStatement()) {
            statement.execute("DROP SCHEMA IF EXISTS " + POSTGRES_SCHEMA + " CASCADE");
            statement.execute("CREATE SCHEMA " + POSTGRES_SCHEMA);
            statement.execute("""
                    CREATE TABLE dataspec_it.user_order (
                        id BIGINT NOT NULL,
                        phone VARCHAR(20),
                        status_code VARCHAR(20),
                        phone_alias VARCHAR(30),
                        user_name VARCHAR(50),
                        raw_payload VARCHAR(100)
                    )
                    """);
            statement.execute("COMMENT ON TABLE dataspec_it.user_order IS '用户订单'");
            statement.execute("COMMENT ON COLUMN dataspec_it.user_order.id IS '主键'");
            statement.execute("COMMENT ON COLUMN dataspec_it.user_order.phone_alias IS '手机号'");
            statement.execute("COMMENT ON COLUMN dataspec_it.user_order.user_name IS '用户名'");
            statement.execute("COMMENT ON COLUMN dataspec_it.user_order.raw_payload IS '扩展信息'");
            statement.execute("CREATE INDEX idx_user_order_phone ON dataspec_it.user_order(phone)");
            statement.execute("CREATE ROLE " + READONLY_USERNAME + " LOGIN PASSWORD '" + POSTGRES_READONLY_PASSWORD + "'");
            statement.execute("GRANT CONNECT ON DATABASE " + POSTGRES.getDatabaseName() + " TO " + READONLY_USERNAME);
            statement.execute("GRANT USAGE ON SCHEMA " + POSTGRES_SCHEMA + " TO " + READONLY_USERNAME);
            statement.execute("GRANT SELECT ON ALL TABLES IN SCHEMA " + POSTGRES_SCHEMA + " TO " + READONLY_USERNAME);
        }
    }

    private static void initializeMysqlFixture() throws Exception {
        try (Connection connection = DriverManager.getConnection(
                MYSQL.getJdbcUrl(),
                MYSQL.getUsername(),
                MYSQL.getPassword());
             Statement statement = connection.createStatement()) {
            statement.execute("DROP TABLE IF EXISTS user_order");
            statement.execute("""
                    CREATE TABLE user_order (
                        id BIGINT NOT NULL COMMENT '主键',
                        phone VARCHAR(20),
                        status_code VARCHAR(20),
                        phone_alias VARCHAR(30) COMMENT '手机号',
                        user_name VARCHAR(50) COMMENT '用户名',
                        raw_payload VARCHAR(100) COMMENT '扩展信息',
                        INDEX idx_user_order_phone (phone)
                    ) COMMENT='用户订单'
                    """);
        }
        org.testcontainers.containers.Container.ExecResult result = MYSQL.execInContainer(
                "mysql",
                "-uroot",
                "-p" + MYSQL.getPassword(),
                "-e",
                "DROP USER IF EXISTS 'dataspec_ro'@'%';"
                        + "CREATE USER 'dataspec_ro'@'%' IDENTIFIED BY '" + MYSQL_READONLY_PASSWORD + "';"
                        + "GRANT SELECT, SHOW VIEW ON `" + MYSQL.getDatabaseName() + "`.* TO 'dataspec_ro'@'%';"
                        + "FLUSH PRIVILEGES;");
        assertThat(result.getExitCode()).as("[MySQL] create read-only user exit code").isZero();
        assertThat(result.getStderr() + result.getStdout()).as("[MySQL] create read-only user output")
                .doesNotContain(MYSQL_READONLY_PASSWORD, MYSQL_OWNER_PASSWORD);
    }

    private DatabaseSchemaTable onlyTable(DatabaseSchemaDump dump, DialectFixture fixture) {
        assertThat(dump.getTables()).as("[%s] dump table count", fixture.label()).hasSize(1);
        DatabaseSchemaTable table = dump.getTables().get(0);
        assertThat(table.getTableName()).as("[%s] dump table name", fixture.label()).isEqualToIgnoringCase(TABLE_NAME);
        return table;
    }

    private DatabaseSchemaColumn column(DatabaseSchemaTable table, String columnName, DialectFixture fixture) {
        return table.getColumns().stream()
                .filter(column -> columnName.equalsIgnoreCase(column.getColumnName()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("[" + fixture.label() + "] missing column " + columnName));
    }

    private static DialectFixture postgresqlFixture() {
        return new DialectFixture(
                "PostgreSQL",
                "postgresql",
                POSTGRES.getDatabaseName(),
                POSTGRES_SCHEMA,
                POSTGRES.getHost(),
                POSTGRES.getMappedPort(PostgreSQLContainer.POSTGRESQL_PORT),
                READONLY_USERNAME,
                POSTGRES_READONLY_PASSWORD);
    }

    private static DialectFixture mysqlFixture() {
        return new DialectFixture(
                "MySQL",
                "mysql",
                MYSQL.getDatabaseName(),
                "ignored_schema",
                MYSQL.getHost(),
                MYSQL.getMappedPort(MySQLContainer.MYSQL_PORT),
                READONLY_USERNAME,
                MYSQL_READONLY_PASSWORD);
    }

    private static String randomCredential() {
        return "it-" + UUID.randomUUID();
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private record DialectFixture(String label,
                                  String databaseType,
                                  String databaseName,
                                  String schemaName,
                                  String host,
                                  Integer port,
                                  String username,
                                  String password) {

        DatabaseConnectionReq request() {
            DatabaseConnectionReq req = new DatabaseConnectionReq();
            req.setProjectId(PROJECT_ID);
            req.setDatabaseType(databaseType);
            req.setHost(host);
            req.setPort(port);
            req.setDatabaseName(databaseName);
            req.setSchemaName(schemaName);
            req.setUsername(username);
            req.setPassword(password);
            req.setTableNames(List.of(TABLE_NAME));
            return req;
        }
    }
}
