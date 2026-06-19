package com.dataspec.reverseimport;

import com.dataspec.field.entity.Field;
import com.dataspec.field.service.FieldService;
import com.dataspec.reverseimport.model.DatabaseConnectionReq;
import com.dataspec.reverseimport.model.DatabaseImportReq;
import com.dataspec.reverseimport.model.DatabaseTableInfo;
import com.dataspec.reverseimport.model.FieldCandidate;
import com.dataspec.reverseimport.model.ReverseImportPreview;
import com.dataspec.reverseimport.service.impl.DatabaseReverseImportServiceImpl;
import com.dataspec.reverseimport.service.impl.ReverseImportServiceImpl;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 数据库直连反向导入测试。
 */
class DatabaseReverseImportServiceTest {

    @Test
    void testConnectionAndListTables_readJdbcMetadata() throws Exception {
        prepareMetadataDatabase();
        FieldService fieldService = mock(FieldService.class);
        DatabaseReverseImportServiceImpl service = service(fieldService);
        DatabaseConnectionReq req = connectionReq();

        assertThat(service.testConnection(req).success()).isTrue();

        List<DatabaseTableInfo> tables = service.listTables(req);

        assertThat(tables).extracting(DatabaseTableInfo::tableName).contains("USER_ORDER");
    }

    @Test
    void preview_readsColumnsAndReusesReverseImportAnalysis() throws Exception {
        prepareMetadataDatabase();
        FieldService fieldService = mock(FieldService.class);
        when(fieldService.listByProject(1L)).thenReturn(List.of(
                standardField("id", null),
                standardField("mobile_no", "phone,mobile")
        ));
        DatabaseReverseImportServiceImpl service = service(fieldService);
        DatabaseConnectionReq req = connectionReq();
        req.setTableNames(List.of("USER_ORDER"));

        ReverseImportPreview preview = service.preview(req);

        assertThat(preview.getSummary().getTableCount()).isEqualTo(1);
        assertThat(preview.getSummary().getColumnCount()).isEqualTo(3);
        assertThat(preview.getFieldCandidates()).extracting(FieldCandidate::getColumnName)
                .containsExactly("USER_NAME");
        assertThat(preview.getMissingComments()).extracting("columnName")
                .contains("ID", "USER_NAME");
        assertThat(preview.getNonStandardFields()).extracting("columnName")
                .containsExactly("USER_NAME");
    }

    @Test
    void importCandidates_createsNewFieldsAndSkipsExistingNames() {
        FieldService fieldService = mock(FieldService.class);
        when(fieldService.listByProject(1L)).thenReturn(List.of(
                standardField("id", null),
                standardField("mobile_no", "phone,mobile")
        ));
        List<Field> created = new ArrayList<>();
        when(fieldService.create(any(Field.class))).thenAnswer(invocation -> {
            Field field = invocation.getArgument(0);
            created.add(field);
            return field;
        });
        ReverseImportServiceImpl reverseImportService = new ReverseImportServiceImpl(
                new com.dataspec.lint.engine.SqlParserService(),
                fieldService);

        DatabaseImportReq req = new DatabaseImportReq();
        req.setProjectId(1L);
        req.setCandidates(List.of(
                new FieldCandidate("USER_ORDER", "ID", "BIGINT", false, null, "主键"),
                new FieldCandidate("USER_ORDER", "USER_NAME", "VARCHAR(50)", true, null, "用户名")
        ));

        var result = reverseImportService.importCandidates(req);

        assertThat(result.getImportedCount()).isEqualTo(1);
        assertThat(result.getSkippedCount()).isEqualTo(1);
        assertThat(created).hasSize(1);
        assertThat(created.get(0).getName()).isEqualTo("USER_NAME");
        assertThat(created.get(0).getProjectId()).isEqualTo(1L);
        assertThat(created.get(0).getCategory()).isEqualTo("USER_ORDER");
        assertThat(created.get(0).getComment()).isEqualTo("用户名");
    }

    private DatabaseReverseImportServiceImpl service(FieldService fieldService) {
        ReverseImportServiceImpl reverseImportService = new ReverseImportServiceImpl(
                new com.dataspec.lint.engine.SqlParserService(),
                fieldService);
        return new DatabaseReverseImportServiceImpl(reverseImportService, req -> openMetadataConnection());
    }

    private void prepareMetadataDatabase() throws Exception {
        try (Connection connection = openMetadataConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("DROP TABLE IF EXISTS user_order");
            statement.execute("""
                    CREATE TABLE user_order (
                        id BIGINT NOT NULL,
                        phone VARCHAR(20),
                        user_name VARCHAR(50)
                    )
                    """);
        }
    }

    private Connection openMetadataConnection() throws SQLException {
        return DriverManager.getConnection("jdbc:h2:mem:reverse_import;MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
    }

    private DatabaseConnectionReq connectionReq() {
        DatabaseConnectionReq req = new DatabaseConnectionReq();
        req.setProjectId(1L);
        req.setDatabaseType("postgresql");
        req.setHost("localhost");
        req.setPort(5432);
        req.setDatabaseName("demo");
        req.setSchemaName("PUBLIC");
        req.setUsername("sa");
        req.setPassword("");
        return req;
    }

    private Field standardField(String name, String aliases) {
        Field field = new Field();
        field.setProjectId(1L);
        field.setName(name);
        field.setAliases(aliases);
        return field;
    }
}
