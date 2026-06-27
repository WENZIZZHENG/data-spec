package com.dataspec.dialect;

import com.dataspec.dialect.model.DialectCapability;
import com.dataspec.dialect.model.DialectDiagnostic;
import com.dataspec.dialect.model.DialectSupportLevel;
import com.dataspec.dialect.service.SqlDialectCompatibilityService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SqlDialectCompatibilityServiceTest {

    private final SqlDialectCompatibilityService service = new SqlDialectCompatibilityService();

    @Test
    void diagnoseSql_detectsMysqlFeaturesAndFixedSqlRisk() {
        List<DialectDiagnostic> diagnostics = service.diagnoseSql("""
                CREATE TABLE `user_order` (
                  `id` BIGINT AUTO_INCREMENT COMMENT '主键',
                  PRIMARY KEY (`id`),
                  KEY `idx_id` (`id`)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户订单';
                """, true);

        assertThat(diagnostics).extracting(DialectDiagnostic::dialect).containsOnly("mysql");
        assertThat(diagnostics).extracting(DialectDiagnostic::code)
                .contains(
                        "MYSQL_DIALECT_INFERRED",
                        "MYSQL_AUTO_INCREMENT_PARTIAL",
                        "MYSQL_INDEX_TABLE_OPTION_PARTIAL",
                        "MYSQL_BACKTICK_IDENTIFIER_PARTIAL",
                        "MYSQL_INLINE_COMMENT_PARTIAL",
                        "MYSQL_FIXED_SQL_REVIEW_REQUIRED");
        assertThat(diagnostics)
                .filteredOn(item -> "MYSQL_FIXED_SQL_REVIEW_REQUIRED".equals(item.code()))
                .singleElement()
                .extracting(DialectDiagnostic::level)
                .isEqualTo(DialectSupportLevel.WARNING);
    }

    @Test
    void diagnoseSql_defaultsToPostgresqlWithNotice() {
        List<DialectDiagnostic> diagnostics = service.diagnoseSql("""
                CREATE TABLE user_order (id bigserial);
                COMMENT ON TABLE user_order IS '用户订单';
                """);

        assertThat(diagnostics).extracting(DialectDiagnostic::code)
                .contains("POSTGRESQL_DIALECT_INFERRED", "POSTGRESQL_COMMENT_ON_SUPPORTED");
        assertThat(diagnostics)
                .filteredOn(item -> item.capability() == DialectCapability.COMMENTS)
                .singleElement()
                .extracting(DialectDiagnostic::level)
                .isEqualTo(DialectSupportLevel.SUPPORTED);
    }

    @Test
    void diagnoseSql_detectsUnsignedOnlyMysqlDdl() {
        List<DialectDiagnostic> diagnostics = service.diagnoseSql("""
                CREATE TABLE user_order (
                    id BIGINT UNSIGNED NOT NULL,
                    KEY idx_id (id)
                );
                """);

        assertThat(diagnostics).extracting(DialectDiagnostic::dialect).containsOnly("mysql");
        assertThat(diagnostics).extracting(DialectDiagnostic::code)
                .contains("MYSQL_DIALECT_INFERRED", "MYSQL_UNSIGNED_TYPE_PARTIAL", "MYSQL_INDEX_TABLE_OPTION_PARTIAL");
    }

    @Test
    void diagnoseSql_reportsMixedDialectAsUnknown() {
        List<DialectDiagnostic> diagnostics = service.diagnoseSql("""
                CREATE TABLE `user_order` (id bigserial AUTO_INCREMENT);
                COMMENT ON TABLE user_order IS '用户订单';
                """);

        assertThat(diagnostics).hasSize(1);
        assertThat(diagnostics.getFirst().dialect()).isEqualTo("unknown");
        assertThat(diagnostics.getFirst().code()).isEqualTo("MIXED_DIALECT_HINT");
    }

    @Test
    void diagnoseDatabase_reportsUnsupportedAndKnownDialects() {
        assertThat(service.diagnoseDatabase("oracle", null))
                .singleElement()
                .extracting(DialectDiagnostic::code)
                .isEqualTo("UNSUPPORTED_DATABASE_DIALECT");

        assertThat(service.diagnoseDatabase("mysql", null))
                .extracting(DialectDiagnostic::code)
                .contains("MYSQL_DATABASE_METADATA", "MYSQL_CATALOG_SCHEMA_BOUNDARY");
    }

    @Test
    void capabilityMatrix_doesNotOverstateMysqlSupport() {
        assertThat(service.capabilityMatrix())
                .filteredOn(item -> "mysql".equals(item.dialect()))
                .extracting(DialectDiagnostic::level)
                .contains(DialectSupportLevel.PARTIAL)
                .doesNotContain(DialectSupportLevel.SUPPORTED);
    }
}
