package com.dataspec.migration;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PostgreSQL 时间列迁移集成测试。
 *
 * <p>该测试只在 {@code db-integration} profile 中运行，用真实 Flyway V30 到 V31 升级验证
 * 历史墙钟值、PostgreSQL 列类型与项目统一使用的 {@link LocalDateTime} 模型兼容。</p>
 */
class TimestampStorageMigrationIT {

    private static final String PASSWORD = "it-" + UUID.randomUUID();
    private static final String EXTERNAL_JDBC_URL = System.getenv("DATASPEC_IT_JDBC_URL");
    private static final String EXTERNAL_USERNAME = System.getenv("DATASPEC_IT_DB_USER");
    private static final String EXTERNAL_PASSWORD = System.getenv("DATASPEC_IT_DB_PASSWORD");
    private static final String EXTERNAL_OPT_IN = System.getenv("DATASPEC_IT_ALLOW_EXTERNAL_DATABASE");
    private static final String EXPECTED_DATABASE = "dataspec_timestamp_it";

    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
            DockerImageName.parse("postgres:17-alpine"))
            .withDatabaseName("dataspec_timestamp_it")
            .withUsername("dataspec_owner")
            .withPassword(PASSWORD);

    private static String jdbcUrl;
    private static String username;
    private static String password;

    @BeforeAll
    static void configureDatabase() throws Exception {
        if (EXTERNAL_JDBC_URL == null || EXTERNAL_JDBC_URL.isBlank()) {
            POSTGRES.start();
            jdbcUrl = POSTGRES.getJdbcUrl();
            username = POSTGRES.getUsername();
            password = POSTGRES.getPassword();
        } else {
            jdbcUrl = EXTERNAL_JDBC_URL;
            username = requireExternalValue(EXTERNAL_USERNAME, "DATASPEC_IT_DB_USER");
            password = requireExternalValue(EXTERNAL_PASSWORD, "DATASPEC_IT_DB_PASSWORD");
            validateExternalDatabase();
        }
    }

    @AfterAll
    static void stopManagedContainer() {
        if (POSTGRES.isRunning()) {
            POSTGRES.stop();
        }
    }

    @Test
    void v30Upgrade_preservesShanghaiWallClockValuesAndLocalDateTimeCompatibility() throws Exception {
        flyway("30").migrate();
        long projectId;
        try (Connection connection = connection(); Statement statement = connection.createStatement()) {
            statement.execute("SET TIME ZONE 'UTC'");
            try (ResultSet resultSet = statement.executeQuery("""
                    INSERT INTO ds_project(name, description, db_type)
                    VALUES ('timestamp-it', 'migration verification', 'POSTGRESQL')
                    RETURNING id
                    """)) {
                assertThat(resultSet.next()).isTrue();
                projectId = resultSet.getLong("id");
            }
            statement.executeUpdate("""
                    INSERT INTO ds_standard_change_log(
                        project_id, target_type, target_id, action, changed_at
                    ) VALUES (
                        %d, 'field', 1, 'update', TIMESTAMPTZ '2026-07-12 12:34:56+08:00'
                    )
                    """.formatted(projectId));
            statement.executeUpdate("""
                    INSERT INTO ds_api_token(
                        name, token_hash, operator_name, last_used_at, disabled_at
                    ) VALUES (
                        'timestamp-it', repeat('a', 64), 'integration-test',
                        TIMESTAMPTZ '2026-07-12 13:45:01+08:00', NULL
                    )
                    """);
        }

        flyway(null).migrate();

        List<String> incompatibleColumns = new ArrayList<>();
        try (Connection connection = connection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("""
                     SELECT table_name, column_name
                     FROM information_schema.columns
                     WHERE table_schema = 'public'
                       AND table_name LIKE 'ds_%'
                       AND data_type = 'timestamp with time zone'
                     ORDER BY table_name, ordinal_position
                     """)) {
            while (resultSet.next()) {
                incompatibleColumns.add(resultSet.getString("table_name") + "." + resultSet.getString("column_name"));
            }
        }

        assertThat(incompatibleColumns)
                .as("application timestamps represented by LocalDateTime")
                .isEmpty();

        try (Connection connection = connection(); Statement statement = connection.createStatement()) {
            try (ResultSet resultSet = statement.executeQuery("""
                    SELECT changed_at
                    FROM ds_standard_change_log
                    WHERE project_id = %d
                    """.formatted(projectId))) {
                assertThat(resultSet.next()).isTrue();
                assertThat(resultSet.getObject("changed_at", LocalDateTime.class))
                        .isEqualTo(LocalDateTime.of(2026, 7, 12, 12, 34, 56));
            }
            try (ResultSet resultSet = statement.executeQuery("""
                    SELECT last_used_at, disabled_at
                    FROM ds_api_token
                    WHERE name = 'timestamp-it'
                    """)) {
                assertThat(resultSet.next()).isTrue();
                assertThat(resultSet.getObject("last_used_at", LocalDateTime.class))
                        .isEqualTo(LocalDateTime.of(2026, 7, 12, 13, 45, 1));
                assertThat(resultSet.getObject("disabled_at", LocalDateTime.class)).isNull();
            }
        }
    }

    private static Flyway flyway(String target) {
        FluentConfiguration configuration = Flyway.configure()
                .dataSource(jdbcUrl, username, password)
                .locations("classpath:db/migration")
                .schemas("public")
                .defaultSchema("public")
                .initSql("SET TIME ZONE 'UTC'");
        if (target != null) {
            configuration.target(target);
        }
        return configuration.load();
    }

    /**
     * 外部入口只允许显式授权的空白一次性数据库，避免集成测试误迁移开发库或共享库。
     */
    private static void validateExternalDatabase() throws Exception {
        if (!Boolean.parseBoolean(EXTERNAL_OPT_IN)) {
            throw new IllegalStateException(
                    "DATASPEC_IT_ALLOW_EXTERNAL_DATABASE=true is required for the external integration database");
        }
        try (Connection connection = connection(); Statement statement = connection.createStatement()) {
            assertThat(connection.getCatalog()).isEqualTo(EXPECTED_DATABASE);
            assertThat(connection.getSchema())
                    .as("external integration database current schema")
                    .isEqualTo("public");
            try (ResultSet resultSet = statement.executeQuery("""
                    SELECT COUNT(*)
                    FROM information_schema.tables
                    WHERE table_schema = 'public'
                    """)) {
                assertThat(resultSet.next()).isTrue();
                assertThat(resultSet.getInt(1))
                        .as("external integration database public schema must be empty")
                        .isZero();
            }
        }
    }

    private static String requireExternalValue(String value, String variableName) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(variableName + " is required for the external integration database");
        }
        return value;
    }

    private static Connection connection() throws Exception {
        return DriverManager.getConnection(jdbcUrl, username, password);
    }
}
