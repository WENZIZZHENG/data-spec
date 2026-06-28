package com.dataspec.reverseimport;

import com.dataspec.common.exception.BizException;
import com.dataspec.reverseimport.model.DatabaseSchemaDump;
import com.dataspec.reverseimport.service.impl.JdbcDatabaseMetadataAdapter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JdbcDatabaseMetadataAdapterTest {

    private final JdbcDatabaseMetadataAdapter adapter = new JdbcDatabaseMetadataAdapter();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void toTableDefs_convertsPostgresqlFixture() throws Exception {
        DatabaseSchemaDump dump = readDump("schema-dump-postgresql.json");

        var tables = adapter.toTableDefs(1L, dump);

        assertThat(tables).hasSize(1);
        assertThat(tables.get(0).getName()).isEqualTo("user_order");
        assertThat(tables.get(0).getColumns())
                .extracting("name", "dataType", "nullable", "comment")
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("id", "BIGINT", false, "主键"),
                        org.assertj.core.groups.Tuple.tuple("mobile_no", "VARCHAR(20)", true, "手机号")
                );
    }

    @Test
    void toTableDefs_keepsMysqlWarningsAndRejectsProjectMismatch() throws Exception {
        DatabaseSchemaDump dump = readDump("schema-dump-mysql.json");

        assertThat(dump.getWarnings()).anyMatch(warning -> warning.contains("MySQL metadata"));
        BizException ex = assertThrows(BizException.class, () -> adapter.toTableDefs(2L, dump));

        assertThat(ex.getMessage()).contains("项目ID不匹配");
    }

    @Test
    void fixtureDoesNotContainSecretsOrJdbcUrl() throws Exception {
        String postgresql = readFixture("schema-dump-postgresql.json");
        String mysql = readFixture("schema-dump-mysql.json");

        assertThat(postgresql + mysql)
                .doesNotContain("password", "Bearer ", "jdbc:");
    }

    private DatabaseSchemaDump readDump(String name) throws Exception {
        return objectMapper.readValue(readFixture(name), DatabaseSchemaDump.class);
    }

    private String readFixture(String name) throws Exception {
        return new String(getClass().getResourceAsStream("/fixtures/reverseimport/" + name).readAllBytes(), StandardCharsets.UTF_8);
    }
}
