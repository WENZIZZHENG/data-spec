package com.dataspec.reverseimport;

import com.dataspec.field.entity.Field;
import com.dataspec.field.service.FieldService;
import com.dataspec.coverage.model.FieldCoverageStatus;
import com.dataspec.common.exception.BizException;
import com.dataspec.common.safety.DryRunEvidenceSigner;
import com.dataspec.coverage.service.impl.FieldCoverageServiceImpl;
import com.dataspec.reverseimport.entity.ReverseImportBatch;
import com.dataspec.reverseimport.model.DatabaseConnectionReq;
import com.dataspec.reverseimport.model.DatabaseConnectionResult;
import com.dataspec.reverseimport.model.DatabaseConnectionSecurityDiagnostic;
import com.dataspec.reverseimport.model.DatabaseCommentPatchPlan;
import com.dataspec.reverseimport.model.DatabaseCommentPatchPlanItem;
import com.dataspec.reverseimport.model.DatabaseImportReq;
import com.dataspec.reverseimport.model.DatabaseMetadataCacheMode;
import com.dataspec.reverseimport.model.DatabaseMetadataBrowser;
import com.dataspec.reverseimport.model.DatabaseMetadataScanReq;
import com.dataspec.reverseimport.model.DatabaseMetadataScanEvidence;
import com.dataspec.reverseimport.model.DatabaseMetadataScanFailureSummary;
import com.dataspec.reverseimport.model.DatabaseMetadataScanRateLimit;
import com.dataspec.reverseimport.model.DatabaseMetadataScanResult;
import com.dataspec.reverseimport.model.DatabaseSchemaDumpReq;
import com.dataspec.reverseimport.model.DatabaseSchemaChangeAction;
import com.dataspec.reverseimport.model.DatabaseSchemaChangeItem;
import com.dataspec.reverseimport.model.DatabaseSchemaChangePlan;
import com.dataspec.reverseimport.model.DatabaseSchemaChangeSummary;
import com.dataspec.reverseimport.model.DatabaseSchemaDump;
import com.dataspec.reverseimport.model.DatabaseSchemaTable;
import com.dataspec.reverseimport.model.DatabaseTableInfo;
import com.dataspec.reverseimport.model.FieldCandidate;
import com.dataspec.reverseimport.model.ReverseImportCompareResult;
import com.dataspec.reverseimport.model.ReverseImportFieldStatus;
import com.dataspec.reverseimport.model.ReverseImportPreview;
import com.dataspec.reverseimport.entity.DatabaseMetadataCacheEntry;
import com.dataspec.reverseimport.repository.DatabaseMetadataCacheRepository;
import com.dataspec.reverseimport.service.DatabaseMetadataAdapter;
import com.dataspec.reverseimport.service.impl.DatabaseMetadataCacheServiceImpl;
import com.dataspec.reverseimport.service.ReverseImportSourceService;
import com.dataspec.reverseimport.service.impl.DatabaseReverseImportServiceImpl;
import com.dataspec.reverseimport.service.impl.ReverseImportServiceImpl;
import com.dataspec.lint.model.ColumnDef;
import com.dataspec.lint.model.TableDef;
import com.dataspec.template.entity.Template;
import com.dataspec.template.service.TemplateService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
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
        DatabaseReverseImportServiceImpl service = service(
                fieldService,
                req -> openMetadataConnection(),
                cacheService(new InMemoryDatabaseMetadataCacheRepository()));
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
            if (sql.contains("has_database_privilege")
                    || sql.contains("has_schema_privilege")
                    || sql.contains("has_table_privilege")) {
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
    void testConnection_marksPostgresqlSelectOnlyAccountSafeWhenWriteRiskIsFalse() throws Exception {
        DatabaseConnectionReq req = connectionReq();
        Connection connection = mock(Connection.class);
        DatabaseMetaData metaData = mock(DatabaseMetaData.class);
        Statement statement = mock(Statement.class);
        when(connection.getMetaData()).thenReturn(metaData);
        when(connection.isReadOnly()).thenReturn(false);
        when(connection.createStatement()).thenReturn(statement);
        when(metaData.getUserName()).thenReturn("dataspec_ro");
        when(metaData.isReadOnly()).thenReturn(false);
        ResultSet schemas = rowCountResult(1);
        ResultSet tables = rowCountResult(1);
        when(metaData.getSchemas()).thenReturn(schemas);
        when(metaData.getTables(any(), any(), any(), any())).thenReturn(tables);
        when(statement.executeQuery(anyString())).thenAnswer(invocation -> {
            String sql = invocation.getArgument(0, String.class).toLowerCase();
            if (sql.contains("current_user")) {
                return stringResult("dataspec_ro");
            }
            if (sql.contains("transaction_read_only")) {
                return stringResult("off");
            }
            if (sql.contains("has_database_privilege")
                    || sql.contains("has_schema_privilege")
                    || sql.contains("has_table_privilege")) {
                return booleanResult(false);
            }
            throw new SQLException("unexpected query: " + sql);
        });
        DatabaseReverseImportServiceImpl service = new DatabaseReverseImportServiceImpl(
                mock(com.dataspec.reverseimport.service.ReverseImportService.class),
                ignored -> connection);

        DatabaseConnectionResult result = service.testConnection(req);

        assertThat(result.success()).isTrue();
        assertThat(result.security().readOnly()).isTrue();
        assertThat(result.security().writeRisk()).isFalse();
        assertThat(result.security().riskLevel()).isEqualTo("SAFE");
        assertThat(result.security().warnings()).isEmpty();
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
        DatabaseReverseImportServiceImpl service = service(
                fieldService,
                req -> openMetadataConnection(),
                cacheService(new InMemoryDatabaseMetadataCacheRepository()));
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
    void exportDump_autoCacheHitSkipsJdbcConnection() throws Exception {
        prepareMetadataDatabase();
        DatabaseMetadataCacheServiceImpl cacheService = cacheService(new InMemoryDatabaseMetadataCacheRepository());
        DatabaseReverseImportServiceImpl service = service(mock(FieldService.class), req -> openMetadataConnection(), cacheService);
        DatabaseConnectionReq req = connectionReq();
        req.setTableNames(List.of("USER_ORDER"));

        var first = service.exportDump(req);

        assertThat(first.getMetadataCache().isCacheHit()).isFalse();
        assertThat(first.getMetadataCache().getMetadataFingerprint()).hasSize(64);

        DatabaseReverseImportServiceImpl hitService = service(mock(FieldService.class), ignored -> {
            throw new SQLException("缓存命中时不应打开 JDBC 连接");
        }, cacheService);

        var second = hitService.exportDump(req);

        assertThat(second.getMetadataCache().isCacheHit()).isTrue();
        assertThat(second.getMetadataCache().getRefreshMode()).isEqualTo(DatabaseMetadataCacheMode.AUTO.name());
        assertThat(second.getMetadataCache().getMetadataFingerprint()).isEqualTo(first.getMetadataCache().getMetadataFingerprint());
        assertThat(second.getTables().get(0).getColumns()).extracting("columnName")
                .containsExactly("ID", "PHONE", "USER_NAME");
    }

    @Test
    void exportDump_failureRedactsConnectionSecrets() {
        DatabaseReverseImportServiceImpl service = service(mock(FieldService.class), ignored -> {
            throw new SQLException("read failed password=top-secret jdbc:postgresql://localhost:5432/demo Bearer token123");
        }, cacheService(new InMemoryDatabaseMetadataCacheRepository()));
        DatabaseConnectionReq req = connectionReq();
        req.setPassword("top-secret");
        req.setTableNames(List.of("USER_ORDER"));

        BizException error = assertThrows(BizException.class, () -> service.exportDump(req));

        assertThat(error.getMessage()).contains("读取数据库表结构失败");
        assertThat(error.getMessage()).doesNotContain("top-secret", "jdbc:postgresql://", "Bearer token123");
    }

    @Test
    void exportDumpAndBrowse_redactsSensitiveMetadataText() throws Exception {
        prepareSensitiveMetadataDatabase();
        FieldService fieldService = mock(FieldService.class);
        when(fieldService.listByProject(1L)).thenReturn(List.of());
        DatabaseReverseImportServiceImpl service = service(
                fieldService,
                req -> openMetadataConnection(),
                cacheService(new InMemoryDatabaseMetadataCacheRepository()));
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
        DatabaseReverseImportServiceImpl service = service(
                fieldService,
                req -> openMetadataConnection(),
                cacheService(new InMemoryDatabaseMetadataCacheRepository()));
        DatabaseConnectionReq req = connectionReq();
        req.setPassword("top-secret");
        req.setTableNames(List.of("USER_ORDER"));

        DatabaseMetadataBrowser browser = service.browse(req);

        assertThat(browser.getKind()).isEqualTo("dataspec-database-metadata-browser");
        assertThat(browser.getMetadataCache().getMetadataFingerprint()).hasSize(64);
        assertThat(browser.getMetadataCache().getRefreshMode()).isEqualTo(DatabaseMetadataCacheMode.AUTO.name());
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
        assertThat(browser.getAiReadableSummary()).contains("metadataFingerprint=");
        assertThat(browser.getAiReadableSummary()).doesNotContain("top-secret", "jdbc:");
        assertThat(browser.getPreview().getFieldCandidates()).extracting(FieldCandidate::getColumnName)
                .containsExactly("USER_NAME");
        assertThat(browser.getPreview().getMetadataCache().getMetadataFingerprint())
                .isEqualTo(browser.getMetadataCache().getMetadataFingerprint());
        assertThat(browser.getCoverage().getSummary().getColumnCount()).isEqualTo(3);
        assertThat(browser.getCoverage().getMetadataCache().getMetadataFingerprint())
                .isEqualTo(browser.getMetadataCache().getMetadataFingerprint());
        verify(fieldService, never()).create(any(Field.class));
    }

    @Test
    void scanMetadata_paginatesLargeDatabaseAndBuildsSafeResumeCommand() throws Exception {
        prepareLargeMetadataDatabase(123);
        FieldService fieldService = mock(FieldService.class);
        DatabaseReverseImportServiceImpl service = service(
                fieldService,
                ignored -> openLargeMetadataConnection(),
                cacheService(new InMemoryDatabaseMetadataCacheRepository()));
        DatabaseMetadataScanReq req = scanReq();
        req.setPassword("top-secret");
        req.setPageSize(40);

        DatabaseMetadataScanResult firstPage = service.scan(req);

        assertThat(firstPage.getKind()).isEqualTo("dataspec-database-metadata-scan");
        assertThat(firstPage.getEstimatedTableCount()).isEqualTo(123);
        assertThat(firstPage.getTables()).hasSize(40);
        assertThat(firstPage.getTables().get(0).tableName()).isEqualTo("SCAN_TABLE_001");
        assertThat(firstPage.getScanJobId()).isEqualTo(firstPage.getScanId());
        assertThat(firstPage.getStatus()).isEqualTo("PARTIAL");
        assertThat(firstPage.getResumeCursor()).isEqualTo("40");
        assertThat(firstPage.getCancelToken()).startsWith("cancel-");
        assertThat(firstPage.getPageSize()).isEqualTo(40);
        assertThat(firstPage.getRateLimit().getEffectivePageSize()).isEqualTo(40);
        assertThat(firstPage.getSourcePressureHint().getLevel()).isEqualTo("INFO");
        assertThat(firstPage.getRetryPolicy().isRetryable()).isTrue();
        assertThat(firstPage.getPartialResult().getSuccessfulTables()).hasSize(40);
        assertThat(firstPage.getPartialResult().getSuccessfulTableNames()).contains("SCAN_TABLE_001", "SCAN_TABLE_040");
        assertThat(firstPage.getPartialResult().isComplete()).isFalse();
        assertThat(firstPage.getFailureSummary().getFailedTableCount()).isZero();
        assertThat(firstPage.getEvidence().getScanJobId()).isEqualTo(firstPage.getScanJobId());
        assertThat(firstPage.getEvidence().isSchemaOnly()).isTrue();
        assertThat(firstPage.getEvidence().isNoSourceWrites()).isTrue();
        assertThat(firstPage.getEvidence().isNoStandardWrites()).isTrue();
        assertThat(firstPage.getProgress().getProcessedTableCount()).isEqualTo(40);
        assertThat(firstPage.getProgress().getRemainingTableEstimate()).isEqualTo(83);
        assertThat(firstPage.getProgress().isHasMore()).isTrue();
        assertThat(firstPage.getCursor()).isEqualTo("40");
        assertThat(firstPage.getResumeCommand()).contains("scanJobId=", "resumeCursor=40", "pageSize=40");
        assertThat(firstPage.getMetadataCache().getRefreshMode()).isEqualTo(DatabaseMetadataCacheMode.AUTO.name());
        assertThat(firstPage.getMetadataCache()).isNotNull();
        assertThat(firstPage.getResumeCommand()).doesNotContain("top-secret", "jdbc:");

        req.setScanJobId(firstPage.getScanJobId());
        req.setResumeCursor(firstPage.getResumeCursor());
        DatabaseMetadataScanResult secondPage = service.scan(req);

        assertThat(secondPage.getScanId()).isEqualTo(firstPage.getScanId());
        assertThat(secondPage.getScanJobId()).isEqualTo(firstPage.getScanJobId());
        assertThat(secondPage.getTables()).hasSize(40);
        assertThat(secondPage.getTables().get(0).tableName()).isEqualTo("SCAN_TABLE_041");
        assertThat(secondPage.getProgress().getProcessedTableCount()).isEqualTo(80);
        verify(fieldService, never()).create(any(Field.class));
    }

    @Test
    void scanMetadata_appliesRateLimitCapAndExplainsSourcePressure() throws Exception {
        prepareLargeMetadataDatabase(150);
        FieldService fieldService = mock(FieldService.class);
        DatabaseReverseImportServiceImpl service = service(fieldService, ignored -> openLargeMetadataConnection());
        DatabaseMetadataScanReq req = scanReq();
        DatabaseMetadataScanRateLimit rateLimit = new DatabaseMetadataScanRateLimit();
        rateLimit.setMaxTablesPerPage(25);
        rateLimit.setMinDelayMs(250);
        req.setPageSize(500);
        req.setRateLimit(rateLimit);

        DatabaseMetadataScanResult result = service.scan(req);

        assertThat(result.getPageSize()).isEqualTo(25);
        assertThat(result.getTables()).hasSize(25);
        assertThat(result.getRateLimit().getRequestedPageSize()).isEqualTo(500);
        assertThat(result.getRateLimit().getRequestedMaxTablesPerPage()).isEqualTo(25);
        assertThat(result.getRateLimit().getMinDelayMs()).isEqualTo(250);
        assertThat(result.getSourcePressureHint().getLevel()).isEqualTo("WARNING");
        assertThat(result.getSourcePressureHint().getMessage()).contains("25");
        assertThat(result.getRetryPolicy().getRetryAfterMs()).isEqualTo(250);
        assertThat(result.getRetryPolicy().isLowerPageSizeRecommended()).isTrue();
        verify(fieldService, never()).create(any(Field.class));
    }

    @Test
    void scanMetadata_refreshesSelectedTablesAndReturnsCacheEvidence() throws Exception {
        prepareLargeMetadataDatabase(3);
        FieldService fieldService = mock(FieldService.class);
        InMemoryDatabaseMetadataCacheRepository repository = new InMemoryDatabaseMetadataCacheRepository();
        DatabaseReverseImportServiceImpl service = service(
                fieldService,
                ignored -> openLargeMetadataConnection(),
                cacheService(repository));
        DatabaseMetadataScanReq req = scanReq();
        req.setPageSize(2);
        req.setTableNames(List.of("SCAN_TABLE_001", "SCAN_TABLE_002"));
        req.setMetadataCacheMode(DatabaseMetadataCacheMode.REFRESH.name());

        DatabaseMetadataScanResult result = service.scan(req);

        assertThat(result.getTables()).hasSize(2);
        assertThat(result.getMetadataCache().getRefreshMode()).isEqualTo(DatabaseMetadataCacheMode.REFRESH.name());
        assertThat(result.getMetadataCache().getMetadataFingerprint()).hasSize(64);
        assertThat(result.getMetadataCache().getChangeSummary().getAddedTableCount()).isEqualTo(2);
        assertThat(repository.entries).hasSize(2);
        verify(fieldService, never()).create(any(Field.class));
    }

    @Test
    void scanMetadata_refreshModeUsesCurrentPageForCacheRefreshWhenRequestContainsMoreTables() throws Exception {
        prepareLargeMetadataDatabase(5);
        FieldService fieldService = mock(FieldService.class);
        InMemoryDatabaseMetadataCacheRepository repository = new InMemoryDatabaseMetadataCacheRepository();
        DatabaseReverseImportServiceImpl service = service(
                fieldService,
                ignored -> openLargeMetadataConnection(),
                cacheService(repository));
        DatabaseMetadataScanReq req = scanReq();
        req.setPageSize(2);
        req.setTableNames(List.of(
                "SCAN_TABLE_001",
                "SCAN_TABLE_002",
                "SCAN_TABLE_003",
                "SCAN_TABLE_004",
                "SCAN_TABLE_005"));
        req.setMetadataCacheMode(DatabaseMetadataCacheMode.REFRESH.name());

        DatabaseMetadataScanResult result = service.scan(req);

        assertThat(result.getTables()).extracting(DatabaseTableInfo::tableName)
                .containsExactly("SCAN_TABLE_001", "SCAN_TABLE_002");
        assertThat(result.getMetadataCache().getRefreshMode()).isEqualTo(DatabaseMetadataCacheMode.REFRESH.name());
        assertThat(repository.entries.values()).extracting(DatabaseMetadataCacheEntry::getTableName)
                .containsExactlyInAnyOrder("SCAN_TABLE_001", "SCAN_TABLE_002");
        assertThat(repository.entries).hasSize(2);
        verify(fieldService, never()).create(any(Field.class));
    }

    @Test
    void scanMetadata_cancelStopsPageAndDoesNotWriteStandardLibrary() throws Exception {
        prepareLargeMetadataDatabase(12);
        FieldService fieldService = mock(FieldService.class);
        DatabaseReverseImportServiceImpl service = service(fieldService, ignored -> openLargeMetadataConnection());
        DatabaseMetadataScanReq req = scanReq();
        req.setScanJobId("scan-demo");
        req.setResumeCursor("5");
        req.setCancelToken("cancel-demo");

        DatabaseMetadataScanResult result = service.scan(req);

        assertThat(result.isCancelled()).isTrue();
        assertThat(result.getScanId()).isEqualTo("scan-demo");
        assertThat(result.getScanJobId()).isEqualTo("scan-demo");
        assertThat(result.getStatus()).isEqualTo("CANCELLED");
        assertThat(result.getTables()).isEmpty();
        assertThat(result.getPartialResult().getSuccessfulTables()).isEmpty();
        assertThat(result.getEvidence().isNoSourceWrites()).isTrue();
        assertThat(result.getEvidence().isNoStandardWrites()).isTrue();
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
    void scanMetadata_keepsPartialResultWhenSingleTableMetadataFails() throws Exception {
        FieldService fieldService = mock(FieldService.class);
        Connection connection = mock(Connection.class);
        DatabaseMetadataAdapter adapter = mock(DatabaseMetadataAdapter.class);
        when(adapter.listTables(eq(connection), any(DatabaseConnectionReq.class))).thenReturn(List.of(
                new DatabaseTableInfo("PUBLIC", "SCAN_OK", "TABLE", "ok"),
                new DatabaseTableInfo("PUBLIC", "BROKEN_TABLE", "TABLE", "password=top-secret jdbc:postgresql://localhost/demo"),
                new DatabaseTableInfo("PUBLIC", "SCAN_NEXT", "TABLE", "next")));
        when(adapter.exportDump(eq(connection), any(DatabaseConnectionReq.class))).thenAnswer(invocation -> {
            DatabaseConnectionReq request = invocation.getArgument(1, DatabaseConnectionReq.class);
            if (request.getTableNames().contains("BROKEN_TABLE")) {
                throw new SQLException("permission denied on BROKEN_TABLE password=top-secret jdbc:postgresql://localhost/demo");
            }
            return dumpForTables(request);
        });
        DatabaseReverseImportServiceImpl service = new DatabaseReverseImportServiceImpl(
                new ReverseImportServiceImpl(
                        new com.dataspec.lint.engine.SqlParserService(),
                        fieldService,
                        mock(ReverseImportSourceService.class)),
                new FieldCoverageServiceImpl(fieldService, new com.dataspec.lint.engine.SqlParserService()),
                ignored -> connection,
                adapter,
                null);
        DatabaseMetadataScanReq req = scanReq();
        req.setPageSize(3);
        req.setPassword("top-secret");

        DatabaseMetadataScanResult result = service.scan(req);

        assertThat(result.getStatus()).isEqualTo("PARTIAL");
        assertThat(result.getPartialResult().getSuccessfulTableNames()).containsExactly("SCAN_NEXT", "SCAN_OK");
        assertThat(result.getPartialResult().getFailedTableNames()).containsExactly("BROKEN_TABLE");
        assertThat(result.getPartialResult().isComplete()).isFalse();
        assertThat(result.getFailureSummary().getFailedTableCount()).isEqualTo(1);
        assertThat(result.getFailureSummary().getFailedTables()).hasSize(1);
        assertThat(result.getFailureSummary().getFailedTables().get(0).getTableName()).isEqualTo("BROKEN_TABLE");
        assertThat(result.getFailureSummary().getFailedTables().get(0).getMessage())
                .doesNotContain("top-secret", "jdbc:postgresql://");
        assertThat(result.getFailureSummary().getSafeNextActions()).anyMatch(action -> action.contains("降低 pageSize"));
        assertThat(result.getEvidence().getFailedTableCount()).isEqualTo(1);
        assertThat(result.getResumeCommand()).doesNotContain("top-secret", "jdbc:postgresql://");
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
        DatabaseReverseImportServiceImpl service = service(
                fieldService,
                req -> openMetadataConnection(),
                cacheService(new InMemoryDatabaseMetadataCacheRepository()));
        DatabaseConnectionReq connectionReq = connectionReq();
        connectionReq.setTableNames(List.of("USER_ORDER"));
        DatabaseSchemaDumpReq dumpReq = new DatabaseSchemaDumpReq();
        dumpReq.setProjectId(1L);
        dumpReq.setDump(service.exportDump(connectionReq));

        ReverseImportPreview preview = service.previewDump(dumpReq);
        ReverseImportCompareResult compare = service.compareDump(dumpReq);
        var coverage = service.coverageDump(dumpReq);

        assertThat(preview.getMetadataCache().getMetadataFingerprint()).hasSize(64);
        assertThat(compare.getMetadataCache().getMetadataFingerprint()).isEqualTo(preview.getMetadataCache().getMetadataFingerprint());
        assertThat(coverage.getMetadataCache().getMetadataFingerprint()).isEqualTo(preview.getMetadataCache().getMetadataFingerprint());
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
        DatabaseReverseImportServiceImpl service = service(
                fieldService,
                req -> openMetadataConnection(),
                cacheService(new InMemoryDatabaseMetadataCacheRepository()));
        DatabaseConnectionReq req = connectionReq();
        req.setTableNames(List.of("USER_ORDER"));

        ReverseImportCompareResult result = service.compare(req);

        assertThat(result.getMetadataCache().getMetadataFingerprint()).hasSize(64);
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
    void planSchemaChange_buildsReadOnlyMigrationDraftFromCompareResult() throws Exception {
        prepareMetadataDatabase();
        FieldService fieldService = mock(FieldService.class);
        Field id = standardField("id", null);
        id.setDataType("BIGINT");
        id.setNullable(false);
        id.setComment("主键");
        Field mobileNo = standardField("mobile_no", "phone,mobile");
        mobileNo.setDataType("VARCHAR");
        mobileNo.setLength(30);
        mobileNo.setNullable(true);
        mobileNo.setComment("手机号");
        when(fieldService.listByProject(1L)).thenReturn(List.of(id, mobileNo));
        DatabaseReverseImportServiceImpl service = service(
                fieldService,
                req -> openMetadataConnection(),
                cacheService(new InMemoryDatabaseMetadataCacheRepository()));
        DatabaseConnectionReq req = connectionReq();
        req.setTableNames(List.of("USER_ORDER"));
        req.setPassword("plan-secret");

        DatabaseSchemaChangePlan plan = service.planSchemaChange(req);

        assertThat(plan.getCurrentSchemaHash()).hasSize(64);
        assertThat(plan.getTargetSpecHash()).hasSize(64);
        assertThat(plan.getRiskLevel()).isEqualTo("BLOCKED");
        assertThat(plan.getSummary().getChangeCount()).isEqualTo(4);
        assertThat(plan.getSummary().getBlockedCount()).isEqualTo(1);
        assertThat(plan.getChangeSet())
                .extracting("tableName", "columnName", "action", "property", "riskLevel")
                .contains(
                        org.assertj.core.groups.Tuple.tuple("USER_ORDER", "ID", DatabaseSchemaChangeAction.ALTER_COMMENT, "comment", "LOW"),
                        org.assertj.core.groups.Tuple.tuple("USER_ORDER", "PHONE", DatabaseSchemaChangeAction.ALTER_COLUMN, "dataType", "MEDIUM"),
                        org.assertj.core.groups.Tuple.tuple("USER_ORDER", "PHONE", DatabaseSchemaChangeAction.ALTER_COMMENT, "comment", "LOW"),
                        org.assertj.core.groups.Tuple.tuple("USER_ORDER", "USER_NAME", DatabaseSchemaChangeAction.DROP_CANDIDATE, "column", "HIGH")
                );
        assertThat(plan.getMigrationSql()).contains("COMMENT ON COLUMN", "-- REVIEW");
        assertThat(plan.getMigrationSql()).contains("-- BLOCKED DROP_CANDIDATE");
        assertThat(plan.getMigrationSql()).contains("\"PUBLIC\".\"USER_ORDER\".\"PHONE\"");
        assertThat(plan.getMigrationSql()).doesNotContain("ALTER TABLE", "DROP COLUMN", "plan-secret", "jdbc:");
        assertThat(plan.getMigrationSql()).doesNotContainPattern("(?m)^(?!\\s*--).*\\bDROP\\b");
        assertThat(plan.getChangeSet())
                .filteredOn(item -> DatabaseSchemaChangeAction.ALTER_COLUMN.equals(item.getAction()))
                .allMatch(item -> item.getMigrationSql().startsWith("-- REVIEW"));
        assertThat(plan.getBlockedReasons()).anyMatch(reason -> reason.contains("USER_ORDER.USER_NAME"));
        assertThat(plan.getManualChecks()).anyMatch(check -> check.contains("PHONE"));
        assertThat(plan.getNextActions()).contains("高风险或阻塞项需要人工确认后再交给迁移工具。");
        verify(fieldService, never()).create(any(Field.class));
    }

    @Test
    void planSchemaChange_usesPostgresqlSchemaPrefixInDraftSql() throws Exception {
        prepareBizSchemaMetadataDatabase();
        FieldService fieldService = mock(FieldService.class);
        Field mobileNo = standardField("mobile_no", "phone,mobile");
        mobileNo.setDataType("VARCHAR");
        mobileNo.setLength(30);
        mobileNo.setNullable(true);
        mobileNo.setComment("手机号");
        when(fieldService.listByProject(1L)).thenReturn(List.of(mobileNo));
        DatabaseReverseImportServiceImpl service = service(
                fieldService,
                req -> openMetadataConnection(),
                cacheService(new InMemoryDatabaseMetadataCacheRepository()));
        DatabaseConnectionReq req = connectionReq();
        req.setSchemaName("BIZ");
        req.setTableNames(List.of("USER_ORDER"));

        DatabaseSchemaChangePlan plan = service.planSchemaChange(req);

        assertThat(plan.getMigrationSql()).contains("\"BIZ\".\"USER_ORDER\".\"PHONE\"");
        assertThat(plan.getMigrationSql()).doesNotContain("COMMENT ON COLUMN \"USER_ORDER\".\"PHONE\"");
    }

    @Test
    void planSchemaChange_usesDefaultPostgresqlSchemaPrefixWhenRequestOmitsSchema() throws Exception {
        prepareDefaultPublicSchemaMetadataDatabase();
        FieldService fieldService = mock(FieldService.class);
        Field mobileNo = standardField("mobile_no", "phone,mobile");
        mobileNo.setDataType("VARCHAR");
        mobileNo.setLength(30);
        mobileNo.setNullable(true);
        mobileNo.setComment("手机号");
        when(fieldService.listByProject(1L)).thenReturn(List.of(mobileNo));
        DatabaseReverseImportServiceImpl service = service(
                fieldService,
                req -> openMetadataConnection(),
                cacheService(new InMemoryDatabaseMetadataCacheRepository()));
        DatabaseConnectionReq req = connectionReq();
        req.setSchemaName(null);
        req.setTableNames(List.of("USER_ORDER"));

        DatabaseSchemaChangePlan plan = service.planSchemaChange(req);

        assertThat(plan.getMigrationSql()).contains("\"public\".\"USER_ORDER\".\"PHONE\"");
        assertThat(plan.getMigrationSql()).doesNotContain("COMMENT ON COLUMN \"USER_ORDER\".\"PHONE\"");
    }

    @Test
    void planSchemaChange_keepsUnsafeStructureChangesAsReviewComments() throws Exception {
        prepareRiskyMetadataDatabase();
        FieldService fieldService = mock(FieldService.class);
        Field phone = standardField("phone", null);
        phone.setDataType("varchar(30); DROP TABLE account; --");
        phone.setNullable(true);
        phone.setDefaultValue("now(); DROP TABLE account; --");
        phone.setComment("手机号");
        when(fieldService.listByProject(1L)).thenReturn(List.of(phone));
        DatabaseReverseImportServiceImpl service = service(
                fieldService,
                req -> openMetadataConnection(),
                cacheService(new InMemoryDatabaseMetadataCacheRepository()));
        DatabaseConnectionReq req = connectionReq();
        req.setTableNames(List.of("RISKY_ORDER"));

        DatabaseSchemaChangePlan plan = service.planSchemaChange(req);

        assertThat(plan.getChangeSet())
                .filteredOn(item -> DatabaseSchemaChangeAction.ALTER_COLUMN.equals(item.getAction()))
                .extracting(DatabaseSchemaChangeItem::getMigrationSql)
                .allMatch(sql -> sql.toString().startsWith("-- REVIEW"));
        assertThat(plan.getMigrationSql())
                .contains("-- REVIEW")
                .doesNotContain("DROP TABLE account", "SET DEFAULT", "DROP DEFAULT", "DROP NOT NULL")
                .doesNotContainPattern("(?m)^(?!\\s*--).*\\bDROP\\b");
    }

    @Test
    void planSchemaChange_keepsMultilineIdentifiersInsideReviewComments() throws Exception {
        prepareMultilineIdentifierMetadataDatabase();
        FieldService fieldService = mock(FieldService.class);
        Field phone = standardField("phone", "phone\nDROP TABLE account; --");
        phone.setDataType("VARCHAR");
        phone.setLength(30);
        phone.setNullable(true);
        phone.setComment("手机号");
        when(fieldService.listByProject(1L)).thenReturn(List.of(phone));
        DatabaseReverseImportServiceImpl service = service(
                fieldService,
                req -> openMetadataConnection(),
                cacheService(new InMemoryDatabaseMetadataCacheRepository()));
        DatabaseConnectionReq req = connectionReq();
        req.setTableNames(List.of("risk_order\nDROP TABLE account; --"));

        DatabaseSchemaChangePlan plan = service.planSchemaChange(req);

        assertThat(plan.getMigrationSql()).contains("-- REVIEW", "-- BLOCKED DROP_CANDIDATE");
        assertThat(plan.getMigrationSql()).doesNotContain("\nDROP TABLE account", "\rDROP TABLE account");
        assertThat(plan.getMigrationSql()).doesNotContainPattern("(?m)^\\s*DROP\\b");
    }

    @Test
    void planSchemaChange_keepsMultilineSchemaNameInsideReviewComments() throws Exception {
        prepareMultilineSchemaMetadataDatabase();
        FieldService fieldService = mock(FieldService.class);
        Field phone = standardField("phone", null);
        phone.setDataType("VARCHAR");
        phone.setLength(30);
        phone.setNullable(true);
        phone.setComment("手机号");
        when(fieldService.listByProject(1L)).thenReturn(List.of(phone));
        DatabaseReverseImportServiceImpl service = service(
                fieldService,
                req -> openMetadataConnection(),
                cacheService(new InMemoryDatabaseMetadataCacheRepository()));
        DatabaseConnectionReq req = connectionReq();
        req.setSchemaName("evil_schema\nDROP TABLE account; --");
        req.setTableNames(List.of("USER_ORDER"));

        DatabaseSchemaChangePlan plan = service.planSchemaChange(req);

        assertThat(plan.getMigrationSql()).contains("\"evil_schema DROP TABLE account; --\".\"USER_ORDER\".\"PHONE\"");
        assertThat(plan.getMigrationSql()).doesNotContain("\nDROP TABLE account", "\rDROP TABLE account");
        assertThat(plan.getMigrationSql()).doesNotContainPattern("(?m)^\\s*DROP\\b");
    }


    @Test
    void schemaChangePlanModelsExposeOpenApiDescriptions() throws Exception {
        assertSchemaDescription(DatabaseSchemaChangeAction.class);
        assertSchemaDescription(DatabaseSchemaChangePlan.class);
        assertSchemaDescription(DatabaseSchemaChangeSummary.class);
        assertSchemaDescription(DatabaseSchemaChangeItem.class);
        assertSchemaDescription(DatabaseSchemaChangePlan.class, "currentSchemaHash");
        assertSchemaDescription(DatabaseSchemaChangePlan.class, "migrationSql");
        assertSchemaDescription(DatabaseSchemaChangePlan.class, "blockedReasons");
        assertSchemaDescription(DatabaseSchemaChangeItem.class, "action");
        assertSchemaDescription(DatabaseSchemaChangeItem.class, "migrationSql");
        assertSchemaDescription(DatabaseSchemaChangeSummary.class, "blockedCount");
        assertSchemaDescription(DatabaseSchemaChangeAction.class.getField("ALTER_COLUMN"));
        assertSchemaDescription(DatabaseSchemaChangeAction.class.getField("DROP_CANDIDATE"));
    }

    @Test
    void commentPatchPlan_buildsPostgresqlReadonlySqlEvidenceAndRedactsSecrets() throws Exception {
        prepareCommentMetadataDatabase();
        FieldService fieldService = mock(FieldService.class);
        Field phone = standardField("phone", "mobile");
        phone.setDisplayName("手机号");
        phone.setComment("买家手机号");
        when(fieldService.listByProject(1L)).thenReturn(List.of(phone));
        TemplateService templateService = mock(TemplateService.class);
        when(templateService.listByProject(1L)).thenReturn(List.of(template("USER_ORDER", "用户订单标准")));
        InMemoryDatabaseMetadataCacheRepository repository = new InMemoryDatabaseMetadataCacheRepository();
        DatabaseReverseImportServiceImpl service = service(
                fieldService,
                req -> openMetadataConnection(),
                cacheService(repository),
                templateService);
        DatabaseConnectionReq req = connectionReq();
        req.setPassword("top-secret");
        req.setTableNames(List.of("USER_ORDER"));

        DatabaseCommentPatchPlan plan = service.planCommentPatch(req);

        assertThat(plan.getKind()).isEqualTo("dataspec-database-comment-patch-plan");
        assertThat(plan.getProjectId()).isEqualTo(1L);
        assertThat(plan.getDatabaseType()).isEqualTo("POSTGRESQL");
        assertThat(plan.getMetadataFingerprint()).hasSize(64);
        assertThat(plan.getPlanHash()).hasSize(64);
        assertThat(plan.getSafety().getReadOnly()).isTrue();
        assertThat(plan.getSafety().getWritesSourceDatabase()).isFalse();
        assertThat(plan.getSafety().getWritesProject()).isFalse();
        assertThat(repository.entries).isEmpty();
        assertThat(plan.getEvidence().getSafetyFlags()).contains("noMetadataCacheWrites");
        assertThat(plan.getEvidence().getNormalizedInputSummary()).contains("metadataCacheMode=BYPASS");
        assertThat(plan.getSummary().getChangedCount()).isEqualTo(2);
        assertThat(plan.getSummary().getExecutableChangeCount()).isEqualTo(2);
        assertThat(plan.getDialectSupport().getTableCommentSqlSupported()).isTrue();
        assertThat(plan.getDialectSupport().getColumnCommentSqlSupported()).isTrue();
        assertThat(plan.getEvidence().getMetadataFingerprint()).isEqualTo(plan.getMetadataFingerprint());
        assertThat(plan.getEvidence().getStandardReferences()).contains("template:USER_ORDER", "field:phone");
        assertThat(plan.getNextActions()).anyMatch(action -> action.contains("dry-run SQL"));

        DatabaseCommentPatchPlanItem tableItem = item(plan, "TABLE", "USER_ORDER", null);
        assertThat(tableItem.getStatus()).isEqualTo("CHANGED");
        assertThat(tableItem.getCurrentComment()).isEqualTo("旧订单表");
        assertThat(tableItem.getTargetComment()).isEqualTo("用户订单标准");
        assertThat(tableItem.getDryRunSql()).contains("COMMENT ON TABLE", "USER_ORDER", "用户订单标准");
        assertThat(tableItem.getRollbackHint()).contains("旧订单表");

        DatabaseCommentPatchPlanItem columnItem = item(plan, "COLUMN", "USER_ORDER", "PHONE");
        assertThat(columnItem.getStatus()).isEqualTo("CHANGED");
        assertThat(columnItem.getCurrentComment()).isEqualTo("旧手机号");
        assertThat(columnItem.getTargetComment()).isEqualTo("买家手机号");
        assertThat(columnItem.getDryRunSql()).contains("COMMENT ON COLUMN", "PHONE", "买家手机号");
        assertThat(columnItem.getEvidenceRefs()).contains("field:phone");

        String json = objectMapper.writeValueAsString(plan);
        assertThat(json).doesNotContain("top-secret", "jdbc:", "token123", "Authorization");
    }

    @Test
    void commentPatchPlan_marksMysqlColumnUnsupportedButKeepsTableSqlSafe() throws Exception {
        FieldService fieldService = mock(FieldService.class);
        Field phone = standardField("phone", "mobile");
        phone.setComment("买家手机号");
        when(fieldService.listByProject(1L)).thenReturn(List.of(phone));
        TemplateService templateService = mock(TemplateService.class);
        when(templateService.listByProject(1L)).thenReturn(List.of(template("user_order", "用户订单标准")));
        DatabaseSchemaDump dump = dumpWithComments("MYSQL", "shop", null, "user_order", "旧订单表", "phone", "旧手机号");
        DatabaseReverseImportServiceImpl service = service(
                fieldService,
                req -> mock(Connection.class),
                new StaticDatabaseMetadataAdapter(dump),
                templateService);
        DatabaseConnectionReq req = connectionReq();
        req.setDatabaseType("mysql");
        req.setDatabaseName("shop");
        req.setSchemaName(null);
        req.setTableNames(List.of("user_order"));

        DatabaseCommentPatchPlan plan = service.planCommentPatch(req);

        assertThat(plan.getRiskLevel()).isEqualTo("MEDIUM");
        assertThat(plan.getSummary().getChangedCount()).isEqualTo(1);
        assertThat(plan.getSummary().getUnsupportedCount()).isEqualTo(1);
        assertThat(plan.getDryRunSql()).contains("ALTER TABLE `user_order` COMMENT = '用户订单标准';");
        assertThat(plan.getDryRunSql()).doesNotContain("MODIFY COLUMN");
        assertThat(plan.getDialectSupport().getTableCommentSqlSupported()).isTrue();
        assertThat(plan.getDialectSupport().getColumnCommentSqlSupported()).isFalse();
        DatabaseCommentPatchPlanItem columnItem = item(plan, "COLUMN", "user_order", "phone");
        assertThat(columnItem.getStatus()).isEqualTo("UNSUPPORTED");
        assertThat(columnItem.getBlockedReasons()).anyMatch(reason -> reason.contains("完整列定义"));
        assertThat(columnItem.getDryRunSql()).isBlank();
    }

    @Test
    void commentPatchPlan_escapesMysqlBackslashAndQuoteInTableCommentSql() throws Exception {
        FieldService fieldService = mock(FieldService.class);
        when(fieldService.listByProject(1L)).thenReturn(List.of());
        TemplateService templateService = mock(TemplateService.class);
        String targetComment = "安全\\'; DROP TABLE audit_log; --";
        when(templateService.listByProject(1L)).thenReturn(List.of(template("user_order", targetComment)));
        DatabaseSchemaDump dump = dumpWithComments("MYSQL", "shop", null, "user_order", "旧订单表", "phone", "旧手机号");
        DatabaseReverseImportServiceImpl service = service(
                fieldService,
                req -> mock(Connection.class),
                new StaticDatabaseMetadataAdapter(dump),
                templateService);
        DatabaseConnectionReq req = connectionReq();
        req.setDatabaseType("mysql");
        req.setDatabaseName("shop");
        req.setSchemaName(null);
        req.setTableNames(List.of("user_order"));

        DatabaseCommentPatchPlan plan = service.planCommentPatch(req);

        DatabaseCommentPatchPlanItem tableItem = item(plan, "TABLE", "user_order", null);
        assertThat(tableItem.getDryRunSql()).contains("安全\\\\''; DROP TABLE audit_log; --");
        assertThat(tableItem.getDryRunSql()).doesNotContain("安全\\'; DROP TABLE audit_log; --");
    }

    @Test
    void commentPatchPlan_returnsNoOpWhenCommentsAlreadyMatchStandards() throws Exception {
        FieldService fieldService = mock(FieldService.class);
        Field phone = standardField("phone", "mobile");
        phone.setComment("买家手机号");
        when(fieldService.listByProject(1L)).thenReturn(List.of(phone));
        TemplateService templateService = mock(TemplateService.class);
        when(templateService.listByProject(1L)).thenReturn(List.of(template("USER_ORDER", "用户订单标准")));
        DatabaseSchemaDump dump = dumpWithComments("POSTGRESQL", "demo", "PUBLIC", "USER_ORDER", "用户订单标准", "PHONE", "买家手机号");
        DatabaseReverseImportServiceImpl service = service(
                fieldService,
                req -> mock(Connection.class),
                new StaticDatabaseMetadataAdapter(dump),
                templateService);
        DatabaseConnectionReq req = connectionReq();
        req.setTableNames(List.of("USER_ORDER"));

        DatabaseCommentPatchPlan plan = service.planCommentPatch(req);

        assertThat(plan.getRiskLevel()).isEqualTo("SAFE");
        assertThat(plan.getSummary().getNoOpCount()).isEqualTo(2);
        assertThat(plan.getSummary().getExecutableChangeCount()).isZero();
        assertThat(plan.getDryRunSql()).contains("no comment changes");
        assertThat(plan.getItems()).extracting(DatabaseCommentPatchPlanItem::getStatus)
                .containsOnly("NO_OP");
    }

    @Test
    void commentPatchPlanModelsExposeOpenApiDescriptions() throws Exception {
        assertSchemaDescription(DatabaseCommentPatchPlan.class);
        assertSchemaDescription(com.dataspec.reverseimport.model.DatabaseCommentPatchPlanSummary.class);
        assertSchemaDescription(DatabaseCommentPatchPlanItem.class);
        assertSchemaDescription(com.dataspec.reverseimport.model.DatabaseCommentDialectSupport.class);
        assertSchemaDescription(com.dataspec.reverseimport.model.DatabaseCommentPatchPlanEvidence.class);
        assertSchemaDescription(com.dataspec.reverseimport.model.DatabaseCommentPatchPlanSafety.class);
        assertSchemaDescription(DatabaseCommentPatchPlan.class, "dryRunSql");
        assertSchemaDescription(DatabaseCommentPatchPlan.class, "dialectSupport");
        assertSchemaDescription(DatabaseCommentPatchPlan.class, "riskLevel");
        assertSchemaDescription(DatabaseCommentPatchPlan.class, "rollbackHint");
        assertSchemaDescription(DatabaseCommentPatchPlan.class, "evidence");
        assertSchemaDescription(DatabaseCommentPatchPlan.class, "safety");
        assertSchemaDescription(DatabaseCommentPatchPlanItem.class, "commentDiff");
        assertSchemaDescription(DatabaseCommentPatchPlanItem.class, "currentComment");
        assertSchemaDescription(DatabaseCommentPatchPlanItem.class, "targetComment");
        assertSchemaDescription(DatabaseCommentPatchPlanItem.class, "dryRunSql");
        assertSchemaDescription(DatabaseCommentPatchPlanItem.class, "dialectSupport");
    }

    @Test
    void databaseMetadataScanJobModelsExposeOpenApiDescriptions() throws Exception {
        assertSchemaDescription(DatabaseMetadataScanReq.class, "scanJobId");
        assertSchemaDescription(DatabaseMetadataScanReq.class, "resumeCursor");
        assertSchemaDescription(DatabaseMetadataScanReq.class, "cancelToken");
        assertSchemaDescription(DatabaseMetadataScanReq.class, "rateLimit");
        assertSchemaDescription(DatabaseMetadataScanResult.class, "status");
        assertSchemaDescription(DatabaseMetadataScanResult.class, "partialResult");
        assertSchemaDescription(DatabaseMetadataScanResult.class, "failureSummary");
        assertSchemaDescription(DatabaseMetadataScanResult.class, "evidence");
        assertSchemaDescription(DatabaseMetadataScanFailureSummary.class, "failedTables");
        assertSchemaDescription(DatabaseMetadataScanEvidence.class, "safeForAiCopy");
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
        DatabaseReverseImportServiceImpl service = service(
                fieldService,
                req -> openMetadataConnection(),
                cacheService(new InMemoryDatabaseMetadataCacheRepository()));
        DatabaseConnectionReq req = connectionReq();
        req.setTableNames(List.of("USER_ORDER"));

        var report = service.coverage(req);

        assertThat(report.getMetadataCache().getMetadataFingerprint()).hasSize(64);
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
        FieldCandidate id = new FieldCandidate("USER_ORDER", "ID", "BIGINT", false, null, "主键");
        FieldCandidate userName = new FieldCandidate("USER_ORDER", "USER_NAME", "VARCHAR(50)", true, null, "用户名");
        req.setCandidates(List.of(id, userName));
        attachDryRunEvidence(req, List.of(id, userName));

        var result = reverseImportService.importCandidates(req, "database-import-candidates-1");

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
        return service(fieldService, connectionProvider, null);
    }

    private DatabaseReverseImportServiceImpl service(FieldService fieldService,
                                                    DatabaseReverseImportServiceImpl.ConnectionProvider connectionProvider,
                                                    DatabaseMetadataCacheServiceImpl cacheService) {
        return service(fieldService, connectionProvider, cacheService, null);
    }

    private DatabaseReverseImportServiceImpl service(FieldService fieldService,
                                                    DatabaseReverseImportServiceImpl.ConnectionProvider connectionProvider,
                                                    DatabaseMetadataCacheServiceImpl cacheService,
                                                    TemplateService templateService) {
        ReverseImportServiceImpl reverseImportService = new ReverseImportServiceImpl(
                new com.dataspec.lint.engine.SqlParserService(),
                fieldService,
                mock(ReverseImportSourceService.class));
        return new DatabaseReverseImportServiceImpl(
                reverseImportService,
                new FieldCoverageServiceImpl(fieldService, new com.dataspec.lint.engine.SqlParserService()),
                connectionProvider,
                new com.dataspec.reverseimport.service.impl.JdbcDatabaseMetadataAdapter(),
                cacheService,
                templateService);
    }

    private DatabaseReverseImportServiceImpl service(FieldService fieldService,
                                                    DatabaseReverseImportServiceImpl.ConnectionProvider connectionProvider,
                                                    DatabaseMetadataAdapter adapter,
                                                    TemplateService templateService) {
        ReverseImportServiceImpl reverseImportService = new ReverseImportServiceImpl(
                new com.dataspec.lint.engine.SqlParserService(),
                fieldService,
                mock(ReverseImportSourceService.class));
        return new DatabaseReverseImportServiceImpl(
                reverseImportService,
                new FieldCoverageServiceImpl(fieldService, new com.dataspec.lint.engine.SqlParserService()),
                connectionProvider,
                adapter,
                null,
                templateService);
    }

    private DatabaseMetadataCacheServiceImpl cacheService(DatabaseMetadataCacheRepository repository) {
        return new DatabaseMetadataCacheServiceImpl(
                repository,
                new ObjectMapper(),
                Clock.fixed(Instant.parse("2026-07-06T10:00:00Z"), ZoneOffset.UTC));
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

    private void prepareCommentMetadataDatabase() throws Exception {
        try (Connection connection = openMetadataConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("DROP TABLE IF EXISTS user_order");
            statement.execute("""
                    CREATE TABLE user_order (
                        id BIGINT NOT NULL,
                        phone VARCHAR(20)
                    )
                    """);
            statement.execute("COMMENT ON TABLE user_order IS '旧订单表'");
            statement.execute("COMMENT ON COLUMN user_order.phone IS '旧手机号'");
        }
    }

    private void prepareBizSchemaMetadataDatabase() throws Exception {
        try (Connection connection = openMetadataConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("CREATE SCHEMA IF NOT EXISTS biz");
            statement.execute("DROP TABLE IF EXISTS biz.user_order");
            statement.execute("""
                    CREATE TABLE biz.user_order (
                        phone VARCHAR(20)
                    )
                    """);
        }
    }

    private void prepareDefaultPublicSchemaMetadataDatabase() throws Exception {
        try (Connection connection = openMetadataConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("CREATE SCHEMA IF NOT EXISTS \"public\"");
            statement.execute("DROP TABLE IF EXISTS \"public\".user_order");
            statement.execute("""
                    CREATE TABLE "public".user_order (
                        phone VARCHAR(20)
                    )
                    """);
        }
    }

    private void prepareRiskyMetadataDatabase() throws Exception {
        try (Connection connection = openMetadataConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("DROP TABLE IF EXISTS risky_order");
            statement.execute("""
                    CREATE TABLE risky_order (
                        phone VARCHAR(20) NOT NULL DEFAULT 'old'
                    )
                    """);
        }
    }

    private void prepareMultilineIdentifierMetadataDatabase() throws Exception {
        try (Connection connection = openMetadataConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("DROP TABLE IF EXISTS \"risk_order\nDROP TABLE account; --\"");
            statement.execute("""
                    CREATE TABLE "risk_order
                    DROP TABLE account; --" (
                        "phone
                    DROP TABLE account; --" VARCHAR(20),
                        unmanaged VARCHAR(20)
                    )
                    """);
        }
    }

    private void prepareMultilineSchemaMetadataDatabase() throws Exception {
        try (Connection connection = openMetadataConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("CREATE SCHEMA IF NOT EXISTS \"evil_schema\nDROP TABLE account; --\"");
            statement.execute("DROP TABLE IF EXISTS \"evil_schema\nDROP TABLE account; --\".user_order");
            statement.execute("""
                    CREATE TABLE "evil_schema
                    DROP TABLE account; --".user_order (
                        phone VARCHAR(20)
                    )
                    """);
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

    private DatabaseSchemaDump dumpForTables(DatabaseConnectionReq req) {
        DatabaseSchemaDump dump = new DatabaseSchemaDump();
        dump.setProjectId(req.getProjectId());
        dump.setDatabaseType(req.getDatabaseType().toUpperCase(Locale.ROOT));
        dump.setDatabaseName(req.getDatabaseName());
        dump.setSchemaName(req.getSchemaName());
        for (String tableName : req.getTableNames()) {
            DatabaseSchemaTable table = new DatabaseSchemaTable();
            table.setSchemaName(req.getSchemaName());
            table.setTableName(tableName);
            table.setTableType("TABLE");
            dump.getTables().add(table);
        }
        return dump;
    }

    private Field standardField(String name, String aliases) {
        Field field = new Field();
        field.setProjectId(1L);
        field.setName(name);
        field.setAliases(aliases);
        return field;
    }

    private Template template(String tablePrefix, String description) {
        Template template = new Template();
        template.setProjectId(1L);
        template.setName(tablePrefix + "模板");
        template.setTablePrefix(tablePrefix);
        template.setDescription(description);
        return template;
    }

    private DatabaseSchemaDump dumpWithComments(String databaseType,
                                                String databaseName,
                                                String schemaName,
                                                String tableName,
                                                String tableComment,
                                                String columnName,
                                                String columnComment) {
        DatabaseSchemaDump dump = new DatabaseSchemaDump();
        dump.setProjectId(1L);
        dump.setDatabaseType(databaseType);
        dump.setDatabaseName(databaseName);
        dump.setSchemaName(schemaName);
        DatabaseSchemaTable table = new DatabaseSchemaTable();
        table.setSchemaName(schemaName);
        table.setTableName(tableName);
        table.setTableType("TABLE");
        table.setComment(tableComment);
        com.dataspec.reverseimport.model.DatabaseSchemaColumn column = new com.dataspec.reverseimport.model.DatabaseSchemaColumn();
        column.setColumnName(columnName);
        column.setDataType("VARCHAR(20)");
        column.setNullable(true);
        column.setComment(columnComment);
        column.setOrdinalPosition(1);
        table.getColumns().add(column);
        dump.getTables().add(table);
        return dump;
    }

    private DatabaseCommentPatchPlanItem item(DatabaseCommentPatchPlan plan,
                                              String objectType,
                                              String tableName,
                                              String columnName) {
        return plan.getItems().stream()
                .filter(candidate -> objectType.equals(candidate.getObjectType()))
                .filter(candidate -> tableName.equals(candidate.getTableName()))
                .filter(candidate -> columnName == null
                        ? candidate.getColumnName() == null
                        : columnName.equals(candidate.getColumnName()))
                .findFirst()
                .orElseThrow();
    }

    private void attachDryRunEvidence(DatabaseImportReq req, List<FieldCandidate> candidates) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("operation", "reverse-import:database-import");
        payload.put("projectId", req.getProjectId());
        payload.put("candidateHashes", candidates.stream()
                .map(this::candidateEvidenceHash)
                .sorted()
                .toList());
        String dryRunToken = DryRunEvidenceSigner.signPayload("rid", payload, objectMapper);
        req.setDryRunToken(dryRunToken);
        candidates.forEach(candidate -> candidate.setDryRunToken(dryRunToken));
    }

    private String candidateEvidenceHash(FieldCandidate candidate) {
        try {
            return DryRunEvidenceSigner.sha256Hex(objectMapper.writeValueAsBytes(List.of(
                    normalize(candidate.getTableName()),
                    normalize(candidate.getColumnName()),
                    nullToEmpty(candidate.getDataType()),
                    String.valueOf(candidate.getNullable()),
                    nullToEmpty(candidate.getDefaultValue()),
                    nullToEmpty(candidate.getComment()))));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private void assertSchemaDescription(Class<?> type) {
        Schema schema = type.getAnnotation(Schema.class);
        assertThat(schema).as(type.getSimpleName() + " @Schema").isNotNull();
        assertThat(schema.description()).as(type.getSimpleName() + " description").isNotBlank();
    }

    private void assertSchemaDescription(Class<?> type, String fieldName) throws NoSuchFieldException {
        assertSchemaDescription(type.getDeclaredField(fieldName));
    }

    private void assertSchemaDescription(java.lang.reflect.Field field) {
        Schema schema = field.getAnnotation(Schema.class);
        assertThat(schema).as(field.getName() + " @Schema").isNotNull();
        assertThat(schema.description()).as(field.getName() + " description").isNotBlank();
    }

    private static class InMemoryDatabaseMetadataCacheRepository implements DatabaseMetadataCacheRepository {

        private final Map<String, DatabaseMetadataCacheEntry> entries = new LinkedHashMap<>();

        @Override
        public Optional<DatabaseMetadataCacheEntry> findActive(Long projectId,
                                                               String sourceScopeHash,
                                                               String schemaName,
                                                               String tableName) {
            return Optional.ofNullable(entries.get(key(projectId, sourceScopeHash, schemaName, tableName)));
        }

        @Override
        public void upsert(DatabaseMetadataCacheEntry entry) {
            entries.put(key(entry.getProjectId(), entry.getSourceScopeHash(), entry.getSchemaName(), entry.getTableName()), entry);
        }

        @Override
        public void expire(DatabaseMetadataCacheEntry entry) {
            entries.put(key(entry.getProjectId(), entry.getSourceScopeHash(), entry.getSchemaName(), entry.getTableName()), entry);
        }

        private String key(Long projectId, String sourceScopeHash, String schemaName, String tableName) {
            return projectId + "|" + sourceScopeHash + "|" + schemaName + "|" + tableName;
        }
    }

    private static class StaticDatabaseMetadataAdapter implements DatabaseMetadataAdapter {

        private final DatabaseSchemaDump dump;

        private StaticDatabaseMetadataAdapter(DatabaseSchemaDump dump) {
            this.dump = dump;
        }

        @Override
        public List<DatabaseTableInfo> listTables(Connection connection, DatabaseConnectionReq req) {
            return dump.getTables().stream()
                    .map(table -> new DatabaseTableInfo(
                            table.getSchemaName(),
                            table.getTableName(),
                            table.getTableType(),
                            table.getComment()))
                    .toList();
        }

        @Override
        public DatabaseSchemaDump exportDump(Connection connection, DatabaseConnectionReq req) {
            return dump;
        }

        @Override
        public List<TableDef> toTableDefs(Long projectId, DatabaseSchemaDump dump) {
            return dump.getTables().stream()
                    .map(table -> TableDef.builder()
                            .name(table.getTableName())
                            .comment(table.getComment())
                            .columns(table.getColumns().stream()
                                    .map(column -> ColumnDef.builder()
                                            .name(column.getColumnName())
                                            .dataType(column.getDataType())
                                            .nullable(Boolean.TRUE.equals(column.getNullable()))
                                            .defaultValue(column.getDefaultValue())
                                            .comment(column.getComment())
                                            .build())
                                    .toList())
                            .build())
                    .toList();
        }
    }

    private record FailureCase(String databaseType,
                               String rawMessage,
                               String category,
                               boolean retryable,
                               String nextActionKeyword,
                               String schemaSupport) {
    }
}
