package com.dataspec.reverseimport;

import com.dataspec.field.entity.Field;
import com.dataspec.field.service.FieldService;
import com.dataspec.coverage.model.FieldCoverageStatus;
import com.dataspec.common.exception.BizException;
import com.dataspec.coverage.service.impl.FieldCoverageServiceImpl;
import com.dataspec.reverseimport.entity.ReverseImportBatch;
import com.dataspec.reverseimport.model.DatabaseConnectionReq;
import com.dataspec.reverseimport.model.DatabaseConnectionResult;
import com.dataspec.reverseimport.model.DatabaseConnectionSecurityDiagnostic;
import com.dataspec.reverseimport.model.DatabaseImportReq;
import com.dataspec.reverseimport.model.DatabaseMetadataBrowser;
import com.dataspec.reverseimport.model.DatabaseMetadataScanReq;
import com.dataspec.reverseimport.model.DatabaseMetadataScanResult;
import com.dataspec.reverseimport.model.DatabaseSchemaDumpReq;
import com.dataspec.reverseimport.model.DatabaseTableInfo;
import com.dataspec.reverseimport.model.FieldCandidate;
import com.dataspec.reverseimport.model.ReverseImportCompareResult;
import com.dataspec.reverseimport.model.ReverseImportFieldStatus;
import com.dataspec.reverseimport.model.ReverseImportPreview;
import com.dataspec.reverseimport.service.ReverseImportSourceService;
import com.dataspec.reverseimport.service.impl.DatabaseReverseImportServiceImpl;
import com.dataspec.reverseimport.service.impl.ReverseImportServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 数据库直连反向导入测试。
 */
class DatabaseReverseImportServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

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
    void testConnection_returnsPostgresqlReadonlySecurityDiagnostic() throws Exception {
        DatabaseConnectionReq req = connectionReq();
        Connection connection = mock(Connection.class);
        DatabaseMetaData metaData = mock(DatabaseMetaData.class);
        Statement statement = mock(Statement.class);
        when(connection.getMetaData()).thenReturn(metaData);
        when(connection.isReadOnly()).thenReturn(true);
        when(connection.createStatement()).thenReturn(statement);
        when(metaData.getUserName()).thenReturn("dataspec_ro");
        when(metaData.isReadOnly()).thenReturn(true);
        ResultSet schemas = rowCountResult(1);
        ResultSet tables = rowCountResult(2);
        when(metaData.getSchemas()).thenReturn(schemas);
        when(metaData.getTables(any(), any(), any(), any())).thenReturn(tables);
        when(statement.executeQuery(anyString())).thenAnswer(invocation -> {
            String sql = invocation.getArgument(0, String.class).toLowerCase();
            if (sql.contains("current_user")) {
                return stringResult("dataspec_ro");
            }
            if (sql.contains("transaction_read_only")) {
                return stringResult("on");
            }
            if (sql.contains("has_database_privilege")) {
                return booleanResult(false);
            }
            throw new SQLException("unexpected query: " + sql);
        });
        DatabaseReverseImportServiceImpl service = new DatabaseReverseImportServiceImpl(
                mock(com.dataspec.reverseimport.service.ReverseImportService.class),
                ignored -> connection);

        DatabaseConnectionResult result = service.testConnection(req);

        assertThat(result.success()).isTrue();
        assertThat(result.health()).isNotNull();
        assertThat(result.health().connectionStatus()).isEqualTo("CONNECTED");
        assertThat(result.health().latencyMs()).isNotNull();
        assertThat(result.health().dialect()).isEqualTo("POSTGRESQL");
        assertThat(result.health().capability().schemaSupport()).isEqualTo("SUPPORTED");
        assertThat(result.health().capability().commentSupport()).isEqualTo("SUPPORTED");
        assertThat(result.health().capability().indexSupport()).isEqualTo("SUPPORTED");
        assertThat(result.health().retryable()).isNull();
        assertThat(result.health().requiredPrivileges()).contains("CONNECT", "USAGE", "SELECT");
        assertThat(result.health().nextActions()).contains("可以继续加载表、反向导入、二次比对或覆盖率报告。");
        DatabaseConnectionSecurityDiagnostic security = result.security();
        assertThat(security).isNotNull();
        assertThat(security.databaseType()).isEqualTo("POSTGRESQL");
        assertThat(security.currentUser()).isEqualTo("dataspec_ro");
        assertThat(security.readOnly()).isTrue();
        assertThat(security.writeRisk()).isFalse();
        assertThat(security.riskLevel()).isEqualTo("SAFE");
        assertThat(security.accessibleSchemaCount()).isEqualTo(1);
        assertThat(security.accessibleTableCount()).isEqualTo(2);
        assertThat(security.recommendedSql()).anyMatch(sql -> sql.contains("GRANT SELECT ON ALL TABLES"));
        verify(statement, never()).execute(anyString());
        verify(statement, never()).executeUpdate(anyString());
    }

    @Test
    void testConnection_returnsMysqlWriteRiskDiagnostic() throws Exception {
        DatabaseConnectionReq req = connectionReq();
        req.setDatabaseType("mysql");
        req.setDatabaseName("shop");
        req.setSchemaName(null);
        Connection connection = mock(Connection.class);
        DatabaseMetaData metaData = mock(DatabaseMetaData.class);
        Statement statement = mock(Statement.class);
        when(connection.getMetaData()).thenReturn(metaData);
        when(connection.isReadOnly()).thenReturn(false);
        when(connection.createStatement()).thenReturn(statement);
        when(metaData.getUserName()).thenReturn("root@%");
        when(metaData.isReadOnly()).thenReturn(false);
        ResultSet catalogs = rowCountResult(1);
        ResultSet tables = rowCountResult(3);
        when(metaData.getCatalogs()).thenReturn(catalogs);
        when(metaData.getTables(any(), any(), any(), any())).thenReturn(tables);
        when(statement.executeQuery(anyString())).thenAnswer(invocation -> {
            String sql = invocation.getArgument(0, String.class).toLowerCase();
            if (sql.contains("show grants")) {
                return stringResult("GRANT SELECT, INSERT, UPDATE ON `shop`.* TO 'root'@'%'");
            }
            if (sql.contains("current_user()")) {
                return stringResult("root@%");
            }
            if (sql.contains("@@read_only") || sql.contains("@@super_read_only")) {
                return stringResult("0");
            }
            throw new SQLException("unexpected query: " + sql);
        });
        DatabaseReverseImportServiceImpl service = new DatabaseReverseImportServiceImpl(
                mock(com.dataspec.reverseimport.service.ReverseImportService.class),
                ignored -> connection);

        DatabaseConnectionResult result = service.testConnection(req);

        assertThat(result.success()).isTrue();
        DatabaseConnectionSecurityDiagnostic security = result.security();
        assertThat(security.databaseType()).isEqualTo("MYSQL");
        assertThat(security.writeRisk()).isTrue();
        assertThat(security.riskLevel()).isEqualTo("DANGER");
        assertThat(security.warnings()).anyMatch(warning -> warning.contains("可能具备写入"));
        assertThat(security.recommendedSql()).contains("GRANT SELECT, SHOW VIEW ON `shop`.* TO 'dataspec_ro'@'%';");
        verify(statement, never()).execute(anyString());
        verify(statement, never()).executeUpdate(anyString());
    }

    @Test
    void testConnection_returnsMysqlReadonlyDiagnosticFromGrants() throws Exception {
        DatabaseConnectionReq req = connectionReq();
        req.setDatabaseType("mysql");
        req.setDatabaseName("shop");
        req.setSchemaName(null);
        Connection connection = mock(Connection.class);
        DatabaseMetaData metaData = mock(DatabaseMetaData.class);
        Statement statement = mock(Statement.class);
        when(connection.getMetaData()).thenReturn(metaData);
        when(connection.isReadOnly()).thenReturn(false);
        when(connection.createStatement()).thenReturn(statement);
        when(metaData.getUserName()).thenReturn("dataspec_ro@%");
        when(metaData.isReadOnly()).thenReturn(false);
        ResultSet catalogs = rowCountResult(1);
        ResultSet tables = rowCountResult(4);
        when(metaData.getCatalogs()).thenReturn(catalogs);
        when(metaData.getTables(any(), any(), any(), any())).thenReturn(tables);
        when(statement.executeQuery(anyString())).thenAnswer(invocation -> {
            String sql = invocation.getArgument(0, String.class).toLowerCase();
            if (sql.contains("show grants")) {
                return stringResult("GRANT SELECT, SHOW VIEW ON `shop`.* TO 'dataspec_ro'@'%'");
            }
            if (sql.contains("current_user()")) {
                return stringResult("dataspec_ro@%");
            }
            if (sql.contains("@@read_only") || sql.contains("@@super_read_only")) {
                return stringResult("0");
            }
            throw new SQLException("unexpected query: " + sql);
        });
        DatabaseReverseImportServiceImpl service = new DatabaseReverseImportServiceImpl(
                mock(com.dataspec.reverseimport.service.ReverseImportService.class),
                ignored -> connection);

        DatabaseConnectionResult result = service.testConnection(req);

        assertThat(result.success()).isTrue();
        DatabaseConnectionSecurityDiagnostic security = result.security();
        assertThat(security.readOnly()).isTrue();
        assertThat(security.writeRisk()).isFalse();
        assertThat(security.riskLevel()).isEqualTo("SAFE");
        assertThat(security.accessibleTableCount()).isEqualTo(4);
        verify(statement, never()).execute(anyString());
        verify(statement, never()).executeUpdate(anyString());
    }

    @Test
    void testConnection_keepsSuccessWhenSecurityDiagnosticQueryFails() throws Exception {
        DatabaseConnectionReq req = connectionReq();
        req.setPassword("secret");
        Connection connection = mock(Connection.class);
        DatabaseMetaData metaData = mock(DatabaseMetaData.class);
        when(connection.getMetaData()).thenReturn(metaData);
        when(connection.isReadOnly()).thenReturn(false);
        when(connection.createStatement()).thenThrow(new SQLException("permission denied password=secret"));
        when(metaData.getUserName()).thenReturn("limited_user");
        when(metaData.isReadOnly()).thenReturn(false);
        ResultSet schemas = rowCountResult(0);
        ResultSet tables = rowCountResult(0);
        when(metaData.getSchemas()).thenReturn(schemas);
        when(metaData.getTables(any(), any(), any(), any())).thenReturn(tables);
        DatabaseReverseImportServiceImpl service = new DatabaseReverseImportServiceImpl(
                mock(com.dataspec.reverseimport.service.ReverseImportService.class),
                ignored -> connection);

        DatabaseConnectionResult result = service.testConnection(req);

        assertThat(result.success()).isTrue();
        assertThat(result.security().riskLevel()).isEqualTo("WARNING");
        assertThat(result.security().warnings()).anyMatch(warning -> warning.contains("[REDACTED]"));
        assertThat(result.security().warnings()).noneMatch(warning -> warning.contains("secret"));
    }

    @Test
    void testConnection_sanitizesFailureMessage() {
        DatabaseConnectionReq req = connectionReq();
        req.setPassword("top-secret");
        DatabaseReverseImportServiceImpl service = new DatabaseReverseImportServiceImpl(
                mock(com.dataspec.reverseimport.service.ReverseImportService.class),
                ignored -> {
                    throw new SQLException("bad jdbc:postgresql://localhost:5432/demo password=top-secret Bearer token123");
                });

        DatabaseConnectionResult result = service.testConnection(req);

        assertThat(result.success()).isFalse();
        assertThat(result.security()).isNull();
        assertThat(result.health()).isNotNull();
        assertThat(result.health().connectionStatus()).isEqualTo("FAILED");
        assertThat(result.health().failureCategory()).isEqualTo("AUTHENTICATION");
        assertThat(result.health().retryable()).isTrue();
        assertThat(result.health().nextActions()).anyMatch(action -> action.contains("用户名或密码"));
        assertThat(result.message()).contains("[REDACTED]");
        assertThat(result.message()).doesNotContain("top-secret", "jdbc:postgresql://localhost:5432/demo", "token123");
        assertThat(result.health().message()).doesNotContain("top-secret", "jdbc:postgresql://localhost:5432/demo", "token123");
    }

    @Test
    void testConnection_classifiesFailureHealthDiagnostics() {
        List<FailureCase> cases = List.of(
                new FailureCase("postgresql",
                        "connect timed out to jdbc:postgresql://localhost:5432/demo Bearer token123",
                        "NETWORK",
                        true,
                        "网络",
                        "SUPPORTED"),
                new FailureCase("postgresql",
                        "database demo does not exist Bearer token123",
                        "SCHEMA_NOT_FOUND",
                        true,
                        "数据库名",
                        "SUPPORTED"),
                new FailureCase("mysql",
                        "permission denied for table user_order Bearer token123",
                        "PERMISSION_DENIED",
                        true,
                        "只读 metadata",
                        "SUPPORTED"),
                new FailureCase("sqlite",
                        "driver not found Bearer token123",
                        "UNSUPPORTED_DIALECT",
                        false,
                        "只支持 PostgreSQL/MySQL",
                        "UNSUPPORTED")
        );

        for (FailureCase item : cases) {
            DatabaseConnectionReq req = connectionReq();
            req.setDatabaseType(item.databaseType());
            req.setPassword("top-secret");
            DatabaseReverseImportServiceImpl service = new DatabaseReverseImportServiceImpl(
                    mock(com.dataspec.reverseimport.service.ReverseImportService.class),
                    ignored -> {
                        throw new SQLException(item.rawMessage());
                    });

            DatabaseConnectionResult result = service.testConnection(req);

            assertThat(result.success()).isFalse();
            assertThat(result.security()).isNull();
            assertThat(result.health().connectionStatus()).isEqualTo("FAILED");
            assertThat(result.health().failureCategory()).isEqualTo(item.category());
            assertThat(result.health().retryable()).isEqualTo(item.retryable());
            assertThat(result.health().capability().schemaSupport()).isEqualTo(item.schemaSupport());
            assertThat(result.health().nextActions()).anyMatch(action -> action.contains(item.nextActionKeyword()));
            assertThat(result.message()).contains("[REDACTED]");
            assertThat(result.message()).doesNotContain("top-secret", "jdbc:postgresql://localhost:5432/demo", "token123");
            assertThat(result.health().message()).doesNotContain("top-secret", "jdbc:postgresql://localhost:5432/demo", "token123");
        }
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
        assertThat(preview.getDialectDiagnostics())
                .extracting("code")
                .contains("POSTGRESQL_DATABASE_METADATA", "POSTGRESQL_SCHEMA_PATTERN");
    }

    @Test
    void exportDump_readsMetadataWithoutSecrets() throws Exception {
        prepareMetadataDatabase();
        DatabaseReverseImportServiceImpl service = service(mock(FieldService.class));
        DatabaseConnectionReq req = connectionReq();
        req.setPassword("top-secret");
        req.setTableNames(List.of("USER_ORDER"));

        var dump = service.exportDump(req);

        assertThat(dump.getKind()).isEqualTo("dataspec-database-schema-dump");
        assertThat(dump.getProjectId()).isEqualTo(1L);
        assertThat(dump.getDatabaseType()).isEqualTo("POSTGRESQL");
        assertThat(dump.getTables()).hasSize(1);
        assertThat(dump.getTables().get(0).getColumns()).extracting("columnName")
                .containsExactly("ID", "PHONE", "USER_NAME");
        assertThat(dump.toString()).doesNotContain("top-secret", "jdbc:");
    }

    @Test
    void exportDumpAndBrowse_redactsSensitiveMetadataText() throws Exception {
        prepareSensitiveMetadataDatabase();
        FieldService fieldService = mock(FieldService.class);
        when(fieldService.listByProject(1L)).thenReturn(List.of());
        DatabaseReverseImportServiceImpl service = service(fieldService);
        DatabaseConnectionReq req = connectionReq();
        req.setPassword("top-secret");
        req.setTableNames(List.of("SECRET_ORDER"));

        var dump = service.exportDump(req);
        DatabaseMetadataBrowser browser = service.browse(req);

        String dumpJson = objectMapper.writeValueAsString(dump);
        String browserJson = objectMapper.writeValueAsString(browser);
        assertThat(dumpJson).contains("[REDACTED]");
        assertThat(browserJson).contains("[REDACTED]");
        assertThat(dumpJson).doesNotContain("top-secret", "token123", "jdbc:postgresql://localhost:5432/demo");
        assertThat(browserJson).doesNotContain("top-secret", "token123", "jdbc:postgresql://localhost:5432/demo");
        assertThat(browser.getAiReadableSummary())
                .doesNotContain("top-secret", "token123", "jdbc:postgresql://localhost:5432/demo");
        assertThat(browser.getPreview().getFieldCandidates())
                .filteredOn(candidate -> candidate.getDefaultValue() != null)
                .extracting(FieldCandidate::getDefaultValue)
                .allSatisfy(defaultValue -> assertThat(defaultValue).doesNotContain("top-secret"));
    }

    @Test
    void browseMetadata_returnsAiSummaryIndexesAndCandidateSelectionWithoutImporting() throws Exception {
        prepareMetadataDatabase();
        FieldService fieldService = mock(FieldService.class);
        Field id = standardField("id", null);
        Field mobileNo = standardField("mobile_no", "phone,mobile");
        mobileNo.setDataType("VARCHAR");
        mobileNo.setLength(30);
        mobileNo.setNullable(true);
        mobileNo.setComment("手机号");
        when(fieldService.listByProject(1L)).thenReturn(List.of(id, mobileNo));
        when(fieldService.suggest(1L, "USER_NAME", 1)).thenReturn(List.of(new com.dataspec.field.model.FieldSuggestion(
                null,
                0,
                "未命中已有标准字段",
                "user_name",
                false)));
        DatabaseReverseImportServiceImpl service = service(fieldService);
        DatabaseConnectionReq req = connectionReq();
        req.setPassword("top-secret");
        req.setTableNames(List.of("USER_ORDER"));

        DatabaseMetadataBrowser browser = service.browse(req);

        assertThat(browser.getKind()).isEqualTo("dataspec-database-metadata-browser");
        assertThat(browser.getSummary().getTableCount()).isEqualTo(1);
        assertThat(browser.getSummary().getColumnCount()).isEqualTo(3);
        assertThat(browser.getSummary().getIndexCount()).isGreaterThanOrEqualTo(1);
        assertThat(browser.getSummary().getCandidateCount()).isEqualTo(1);
        assertThat(browser.getSummary().getChangedCount()).isGreaterThanOrEqualTo(1);
        assertThat(browser.getSelectedTableNames()).containsExactly("USER_ORDER");
        assertThat(browser.getTables()).hasSize(1);
        assertThat(browser.getTables().get(0).getIndexes()).extracting("indexName")
                .contains("IDX_USER_ORDER_PHONE");
        assertThat(browser.getTables().get(0).getColumns())
                .filteredOn(column -> "USER_NAME".equals(column.getColumnName()))
                .singleElement()
                .satisfies(column -> {
                    assertThat(column.isImportCandidate()).isTrue();
                    assertThat(column.isSelectedByDefault()).isTrue();
                    assertThat(column.getCandidateKey()).isEqualTo("USER_ORDER.USER_NAME");
                });
        assertThat(browser.getAiReadableSummary()).contains("USER_ORDER", "USER_NAME", "IDX_USER_ORDER_PHONE");
        assertThat(browser.getAiReadableSummary()).doesNotContain("top-secret", "jdbc:");
        assertThat(browser.getPreview().getFieldCandidates()).extracting(FieldCandidate::getColumnName)
                .containsExactly("USER_NAME");
        assertThat(browser.getCoverage().getSummary().getColumnCount()).isEqualTo(3);
        verify(fieldService, never()).create(any(Field.class));
    }

    @Test
    void scanMetadata_paginatesLargeDatabaseAndBuildsSafeResumeCommand() throws Exception {
        prepareLargeMetadataDatabase(123);
        FieldService fieldService = mock(FieldService.class);
        DatabaseReverseImportServiceImpl service = service(fieldService, ignored -> openLargeMetadataConnection());
        DatabaseMetadataScanReq req = scanReq();
        req.setPassword("top-secret");
        req.setPageSize(40);

        DatabaseMetadataScanResult firstPage = service.scan(req);

        assertThat(firstPage.getKind()).isEqualTo("dataspec-database-metadata-scan");
        assertThat(firstPage.getEstimatedTableCount()).isEqualTo(123);
        assertThat(firstPage.getTables()).hasSize(40);
        assertThat(firstPage.getTables().get(0).tableName()).isEqualTo("SCAN_TABLE_001");
        assertThat(firstPage.getProgress().getProcessedTableCount()).isEqualTo(40);
        assertThat(firstPage.getProgress().getRemainingTableEstimate()).isEqualTo(83);
        assertThat(firstPage.getProgress().isHasMore()).isTrue();
        assertThat(firstPage.getCursor()).isEqualTo("40");
        assertThat(firstPage.getResumeCommand()).contains("cursor=40", "pageSize=40");
        assertThat(firstPage.getResumeCommand()).doesNotContain("top-secret", "jdbc:");

        req.setScanId(firstPage.getScanId());
        req.setCursor(firstPage.getCursor());
        DatabaseMetadataScanResult secondPage = service.scan(req);

        assertThat(secondPage.getScanId()).isEqualTo(firstPage.getScanId());
        assertThat(secondPage.getTables()).hasSize(40);
        assertThat(secondPage.getTables().get(0).tableName()).isEqualTo("SCAN_TABLE_041");
        assertThat(secondPage.getProgress().getProcessedTableCount()).isEqualTo(80);
        verify(fieldService, never()).create(any(Field.class));
    }

    @Test
    void scanMetadata_cancelStopsPageAndDoesNotWriteStandardLibrary() throws Exception {
        prepareLargeMetadataDatabase(12);
        FieldService fieldService = mock(FieldService.class);
        DatabaseReverseImportServiceImpl service = service(fieldService, ignored -> openLargeMetadataConnection());
        DatabaseMetadataScanReq req = scanReq();
        req.setScanId("scan-demo");
        req.setCursor("5");
        req.setCancel(true);

        DatabaseMetadataScanResult result = service.scan(req);

        assertThat(result.isCancelled()).isTrue();
        assertThat(result.getScanId()).isEqualTo("scan-demo");
        assertThat(result.getTables()).isEmpty();
        assertThat(result.getProgress().isHasMore()).isFalse();
        assertThat(result.getNextActions()).anyMatch(action -> action.contains("已取消"));
        verify(fieldService, never()).create(any(Field.class));
    }

    @Test
    void scanMetadata_failureRedactsConnectionSecrets() {
        FieldService fieldService = mock(FieldService.class);
        DatabaseReverseImportServiceImpl service = service(fieldService, ignored -> {
            throw new SQLException("login failed top-secret jdbc:postgresql://localhost:5432/demo Bearer token123");
        });
        DatabaseMetadataScanReq req = scanReq();
        req.setPassword("top-secret");

        BizException error = assertThrows(BizException.class, () -> service.scan(req));

        assertThat(error.getMessage()).contains("读取数据库表失败");
        assertThat(error.getMessage()).doesNotContain("top-secret", "jdbc:postgresql://", "Bearer token123");
        verify(fieldService, never()).create(any(Field.class));
    }

    @Test
    void scanMetadata_currentPageTablesCanDrivePartialBrowserPreview() throws Exception {
        prepareLargeMetadataDatabase(6);
        FieldService fieldService = mock(FieldService.class);
        when(fieldService.listByProject(1L)).thenReturn(List.of());
        DatabaseReverseImportServiceImpl service = service(fieldService, ignored -> openLargeMetadataConnection());
        DatabaseMetadataScanReq scanReq = scanReq();
        scanReq.setPageSize(2);

        DatabaseMetadataScanResult scan = service.scan(scanReq);
        DatabaseConnectionReq browserReq = connectionReq();
        browserReq.setDatabaseName("large");
        browserReq.setSchemaName("PUBLIC");
        browserReq.setTableNames(scan.getTables().stream()
                .map(DatabaseTableInfo::tableName)
                .toList());

        DatabaseMetadataBrowser browser = service.browse(browserReq);

        assertThat(browser.getSummary().getTableCount()).isEqualTo(2);
        assertThat(browser.getSelectedTableNames()).containsExactly("SCAN_TABLE_001", "SCAN_TABLE_002");
        assertThat(browser.getSummary().getColumnCount()).isEqualTo(4);
        verify(fieldService, never()).create(any(Field.class));
    }

    @Test
    void dumpInput_replaysPreviewCompareAndCoverage() throws Exception {
        prepareMetadataDatabase();
        FieldService fieldService = mock(FieldService.class);
        Field id = standardField("id", null);
        Field mobileNo = standardField("mobile_no", "phone,mobile");
        when(fieldService.listByProject(1L)).thenReturn(List.of(id, mobileNo));
        when(fieldService.suggest(1L, "USER_NAME", 1)).thenReturn(List.of(new com.dataspec.field.model.FieldSuggestion(
                null,
                0,
                "未命中已有标准字段",
                "user_name",
                false)));
        DatabaseReverseImportServiceImpl service = service(fieldService);
        DatabaseConnectionReq connectionReq = connectionReq();
        connectionReq.setTableNames(List.of("USER_ORDER"));
        DatabaseSchemaDumpReq dumpReq = new DatabaseSchemaDumpReq();
        dumpReq.setProjectId(1L);
        dumpReq.setDump(service.exportDump(connectionReq));

        ReverseImportPreview preview = service.previewDump(dumpReq);
        ReverseImportCompareResult compare = service.compareDump(dumpReq);
        var coverage = service.coverageDump(dumpReq);

        assertThat(preview.getSummary().getColumnCount()).isEqualTo(3);
        assertThat(compare.getSummary().getColumnCount()).isEqualTo(3);
        assertThat(compare.getSummary().getNewCount()).isEqualTo(1);
        assertThat(coverage.getSummary().getColumnCount()).isEqualTo(3);
        assertThat(coverage.getSummary().getUnmanagedCount()).isEqualTo(1);
    }

    @Test
    void compare_readsMetadataAndReusesReverseImportComparison() throws Exception {
        prepareMetadataDatabase();
        FieldService fieldService = mock(FieldService.class);
        Field id = standardField("id", null);
        id.setDataType("BIGINT");
        id.setNullable(false);
        Field mobileNo = standardField("mobile_no", "phone,mobile");
        mobileNo.setDataType("VARCHAR");
        mobileNo.setLength(30);
        mobileNo.setNullable(true);
        mobileNo.setComment("手机号");
        when(fieldService.listByProject(1L)).thenReturn(List.of(id, mobileNo));
        DatabaseReverseImportServiceImpl service = service(fieldService);
        DatabaseConnectionReq req = connectionReq();
        req.setTableNames(List.of("USER_ORDER"));

        ReverseImportCompareResult result = service.compare(req);

        assertThat(result.getSummary().getTableCount()).isEqualTo(1);
        assertThat(result.getSummary().getColumnCount()).isEqualTo(3);
        assertThat(result.getSummary().getMatchedCount()).isEqualTo(2);
        assertThat(result.getSummary().getChangedCount()).isEqualTo(1);
        assertThat(result.getSummary().getNewCount()).isEqualTo(1);
        assertThat(result.getSummary().getNonStandardCount()).isEqualTo(1);
        assertThat(result.getTableDiffs().get(0).getFieldDiffs())
                .extracting("columnName", "status")
                .contains(
                        org.assertj.core.groups.Tuple.tuple("PHONE", ReverseImportFieldStatus.CHANGED),
                        org.assertj.core.groups.Tuple.tuple("USER_NAME", ReverseImportFieldStatus.NEW)
                );
        assertThat(result.getTableDiffs().get(0).getFieldDiffs())
                .filteredOn(diff -> "PHONE".equals(diff.getColumnName()))
                .singleElement()
                .extracting("standardFieldName")
                .isEqualTo("mobile_no");
    }

    @Test
    void coverage_readsMetadataAndBuildsFieldCoverageReport() throws Exception {
        prepareMetadataDatabase();
        FieldService fieldService = mock(FieldService.class);
        Field id = standardField("id", null);
        Field mobileNo = standardField("mobile_no", "phone,mobile");
        when(fieldService.listByProject(1L)).thenReturn(List.of(id, mobileNo));
        when(fieldService.suggest(1L, "USER_NAME", 1)).thenReturn(List.of(new com.dataspec.field.model.FieldSuggestion(
                null,
                0,
                "未命中已有标准字段",
                "user_name",
                false)));
        DatabaseReverseImportServiceImpl service = service(fieldService);
        DatabaseConnectionReq req = connectionReq();
        req.setTableNames(List.of("USER_ORDER"));

        var report = service.coverage(req);

        assertThat(report.getSummary().getColumnCount()).isEqualTo(3);
        assertThat(report.getSummary().getCoveredCount()).isEqualTo(2);
        assertThat(report.getSummary().getUnmanagedCount()).isEqualTo(1);
        assertThat(report.getTables().get(0).getFields())
                .extracting("columnName", "status")
                .contains(
                        org.assertj.core.groups.Tuple.tuple("ID", FieldCoverageStatus.MISSING_COMMENT),
                        org.assertj.core.groups.Tuple.tuple("PHONE", FieldCoverageStatus.MISSING_COMMENT),
                        org.assertj.core.groups.Tuple.tuple("USER_NAME", FieldCoverageStatus.UNMANAGED)
                );
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
        ReverseImportSourceService sourceService = mock(ReverseImportSourceService.class);
        ReverseImportBatch batch = new ReverseImportBatch();
        batch.setId(11L);
        when(sourceService.createDatabaseBatch(any(DatabaseImportReq.class), eq(1), eq(1)))
                .thenReturn(batch);
        ReverseImportServiceImpl reverseImportService = new ReverseImportServiceImpl(
                new com.dataspec.lint.engine.SqlParserService(),
                fieldService,
                sourceService);

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
        return service(fieldService, req -> openMetadataConnection());
    }

    private DatabaseReverseImportServiceImpl service(FieldService fieldService,
                                                    DatabaseReverseImportServiceImpl.ConnectionProvider connectionProvider) {
        ReverseImportServiceImpl reverseImportService = new ReverseImportServiceImpl(
                new com.dataspec.lint.engine.SqlParserService(),
                fieldService,
                mock(ReverseImportSourceService.class));
        return new DatabaseReverseImportServiceImpl(
                reverseImportService,
                new FieldCoverageServiceImpl(fieldService, new com.dataspec.lint.engine.SqlParserService()),
                connectionProvider);
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
            statement.execute("CREATE INDEX idx_user_order_phone ON user_order(phone)");
        }
    }

    private void prepareSensitiveMetadataDatabase() throws Exception {
        try (Connection connection = openMetadataConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("DROP TABLE IF EXISTS secret_order");
            statement.execute("""
                    CREATE TABLE secret_order (
                        id BIGINT NOT NULL,
                        api_token VARCHAR(100) DEFAULT 'top-secret',
                        connection_note VARCHAR(200)
                    )
                    """);
            statement.execute("""
                    COMMENT ON TABLE secret_order
                    IS 'jdbc:postgresql://localhost:5432/demo password=top-secret'
                    """);
            statement.execute("""
                    COMMENT ON COLUMN secret_order.api_token
                    IS 'Bearer token123 uses top-secret'
                    """);
            statement.execute("CREATE INDEX idx_secret_order_token ON secret_order(api_token)");
        }
    }

    private void prepareLargeMetadataDatabase(int tableCount) throws Exception {
        try (Connection connection = openLargeMetadataConnection();
             Statement statement = connection.createStatement()) {
            for (int i = 1; i <= 150; i++) {
                statement.execute("DROP TABLE IF EXISTS scan_table_" + String.format("%03d", i));
            }
            for (int i = 1; i <= tableCount; i++) {
                statement.execute("""
                        CREATE TABLE scan_table_%03d (
                            id BIGINT NOT NULL,
                            payload VARCHAR(50)
                        )
                        """.formatted(i));
            }
        }
    }

    private Connection openMetadataConnection() throws SQLException {
        return DriverManager.getConnection("jdbc:h2:mem:reverse_import;MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
    }

    private Connection openLargeMetadataConnection() throws SQLException {
        return DriverManager.getConnection("jdbc:h2:mem:reverse_import_large;MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
    }

    private ResultSet rowCountResult(int count) throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        AtomicInteger index = new AtomicInteger();
        when(rs.next()).thenAnswer(invocation -> index.getAndIncrement() < count);
        return rs;
    }

    private ResultSet stringResult(String... values) throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        AtomicInteger index = new AtomicInteger(-1);
        when(rs.next()).thenAnswer(invocation -> index.incrementAndGet() < values.length);
        when(rs.getString(1)).thenAnswer(invocation -> values[index.get()]);
        return rs;
    }

    private ResultSet booleanResult(boolean value) throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.next()).thenReturn(true, false);
        when(rs.getBoolean(1)).thenReturn(value);
        return rs;
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

    private DatabaseMetadataScanReq scanReq() {
        DatabaseMetadataScanReq req = new DatabaseMetadataScanReq();
        req.setProjectId(1L);
        req.setDatabaseType("postgresql");
        req.setHost("localhost");
        req.setPort(5432);
        req.setDatabaseName("large");
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

    private record FailureCase(String databaseType,
                               String rawMessage,
                               String category,
                               boolean retryable,
                               String nextActionKeyword,
                               String schemaSupport) {
    }
}
