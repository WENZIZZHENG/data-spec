package com.dataspec.reverseimport;

import com.dataspec.field.entity.Field;
import com.dataspec.field.service.FieldService;
import com.dataspec.lint.engine.SqlParserService;
import com.dataspec.lint.model.ColumnDef;
import com.dataspec.lint.model.TableDef;
import com.dataspec.reverseimport.entity.ReverseImportBatch;
import com.dataspec.reverseimport.model.DatabaseImportReq;
import com.dataspec.reverseimport.model.FieldCandidate;
import com.dataspec.reverseimport.model.ReverseImportCompareResult;
import com.dataspec.reverseimport.model.ReverseImportFieldStatus;
import com.dataspec.reverseimport.model.ReverseImportPreview;
import com.dataspec.reverseimport.service.ReverseImportSourceService;
import com.dataspec.reverseimport.service.impl.ReverseImportServiceImpl;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * SQL 反向导入预览测试。
 */
class ReverseImportServiceTest {

    @Test
    void preview_returnsCandidatesMissingCommentsAndAliasMatches() {
        FieldService fieldService = mock(FieldService.class);
        when(fieldService.listByProject(1L)).thenReturn(List.of(
                standardField("id", null),
                standardField("mobile_no", "phone,mobile")
        ));
        ReverseImportServiceImpl service = new ReverseImportServiceImpl(
                new SqlParserService(),
                fieldService,
                mock(ReverseImportSourceService.class));

        String sql = """
                CREATE TABLE user_order (
                    id bigint NOT NULL,
                    phone varchar(20),
                    user_name varchar(50)
                );
                COMMENT ON TABLE user_order IS '用户订单';
                COMMENT ON COLUMN user_order.phone IS '手机号';
                """;

        ReverseImportPreview preview = service.preview(1L, sql);

        assertThat(preview.getSummary().getTableCount()).isEqualTo(1);
        assertThat(preview.getSummary().getColumnCount()).isEqualTo(3);
        assertThat(preview.getFieldCandidates()).extracting("columnName").containsExactly("user_name");
        assertThat(preview.getMissingComments()).extracting("columnName").containsExactlyInAnyOrder("id", "user_name");
        assertThat(preview.getNonStandardFields()).extracting("columnName").containsExactly("user_name");
        assertThat(preview.getDialectDiagnostics())
                .extracting("code")
                .contains("POSTGRESQL_DIALECT_INFERRED", "POSTGRESQL_COMMENT_ON_SUPPORTED");
    }

    @Test
    void preview_returnsMysqlDialectDiagnosticsForMysqlDdl() {
        FieldService fieldService = mock(FieldService.class);
        when(fieldService.listByProject(1L)).thenReturn(List.of());
        ReverseImportServiceImpl service = new ReverseImportServiceImpl(
                new SqlParserService(),
                fieldService,
                mock(ReverseImportSourceService.class));

        ReverseImportPreview preview = service.preview(1L, """
                CREATE TABLE `user_order` (
                    `id` bigint AUTO_INCREMENT COMMENT '主键',
                    `phone` varchar(20) COMMENT '手机号',
                    PRIMARY KEY (`id`),
                    KEY `idx_phone` (`phone`)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户订单';
                """);

        assertThat(preview.getDialectDiagnostics())
                .extracting("code")
                .contains(
                        "MYSQL_DIALECT_INFERRED",
                        "MYSQL_AUTO_INCREMENT_PARTIAL",
                        "MYSQL_INDEX_TABLE_OPTION_PARTIAL",
                        "MYSQL_BACKTICK_IDENTIFIER_PARTIAL",
                        "MYSQL_INLINE_COMMENT_PARTIAL");
    }

    @Test
    void compareTables_reportsAliasMatchChangesNewFieldsAndMissingComments() {
        FieldService fieldService = mock(FieldService.class);
        Field id = standardField("id", null);
        id.setDataType("bigint");
        id.setNullable(false);
        id.setComment("主键");
        Field mobileNo = standardField("mobile_no", "phone,mobile");
        mobileNo.setDisplayName("手机号");
        mobileNo.setDataType("varchar");
        mobileNo.setLength(20);
        mobileNo.setNullable(true);
        mobileNo.setComment("手机号");
        Field amount = standardField("amount_cent", null);
        amount.setDataType("bigint");
        amount.setNullable(false);
        amount.setDefaultValue("0");
        amount.setComment("金额分");
        Field createdAt = standardField("created_at", null);
        createdAt.setDataType("timestamp");
        createdAt.setNullable(true);
        createdAt.setComment("创建时间");
        when(fieldService.listByProject(1L)).thenReturn(List.of(id, mobileNo, amount, createdAt));
        ReverseImportServiceImpl service = new ReverseImportServiceImpl(
                new SqlParserService(),
                fieldService,
                mock(ReverseImportSourceService.class));

        TableDef table = TableDef.builder()
                .name("user_order")
                .comment("用户订单")
                .columns(List.of(
                        column("id", "BIGINT", false, null, "主键"),
                        column("phone", "VARCHAR(20)", true, null, "手机号"),
                        column("amount_cent", "BIGINT", true, null, ""),
                        column("created_at", "TIMESTAMP", true, null, ""),
                        column("user_name", "VARCHAR(50)", true, null, "用户名")
                ))
                .build();

        ReverseImportCompareResult result = service.compareTables(1L, List.of(table));

        assertThat(result.getSummary().getTableCount()).isEqualTo(1);
        assertThat(result.getSummary().getColumnCount()).isEqualTo(5);
        assertThat(result.getSummary().getMatchedCount()).isEqualTo(4);
        assertThat(result.getSummary().getChangedCount()).isEqualTo(1);
        assertThat(result.getSummary().getNewCount()).isEqualTo(1);
        assertThat(result.getSummary().getMissingCommentCount()).isEqualTo(2);
        assertThat(result.getSummary().getNonStandardCount()).isEqualTo(1);
        assertThat(result.getTableDiffs()).hasSize(1);
        assertThat(result.getTableDiffs().get(0).getFieldDiffs())
                .extracting("columnName", "status")
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("id", ReverseImportFieldStatus.MATCHED),
                        org.assertj.core.groups.Tuple.tuple("phone", ReverseImportFieldStatus.MATCHED),
                        org.assertj.core.groups.Tuple.tuple("amount_cent", ReverseImportFieldStatus.CHANGED),
                        org.assertj.core.groups.Tuple.tuple("created_at", ReverseImportFieldStatus.MISSING_COMMENT),
                        org.assertj.core.groups.Tuple.tuple("user_name", ReverseImportFieldStatus.NEW)
                );
        assertThat(result.getTableDiffs().get(0).getFieldDiffs().get(1).getStandardFieldName())
                .isEqualTo("mobile_no");
        assertThat(result.getTableDiffs().get(0).getFieldDiffs().get(2).getChanges())
                .extracting("property")
                .containsExactly("nullable", "defaultValue", "comment");
        assertThat(result.getTableDiffs().get(0).getFieldDiffs())
                .filteredOn(diff -> "user_name".equals(diff.getColumnName()))
                .singleElement()
                .extracting("nonStandard")
                .isEqualTo(true);
    }

    @Test
    void importCandidates_recordsDatabaseBatchAndSourcesForNewFields() {
        FieldService fieldService = mock(FieldService.class);
        ReverseImportSourceService sourceService = mock(ReverseImportSourceService.class);
        when(fieldService.listByProject(1L)).thenReturn(List.of(standardField("id", null)));
        when(fieldService.create(any(Field.class))).thenAnswer(invocation -> {
            Field field = invocation.getArgument(0);
            field.setId(99L);
            return field;
        });
        ReverseImportBatch batch = new ReverseImportBatch();
        batch.setId(7L);
        when(sourceService.createDatabaseBatch(any(DatabaseImportReq.class), eq(1), eq(1)))
                .thenReturn(batch);
        ReverseImportServiceImpl service = new ReverseImportServiceImpl(
                new SqlParserService(),
                fieldService,
                sourceService);
        DatabaseImportReq req = databaseImportReq(List.of(
                new FieldCandidate("USER_ORDER", "id", "BIGINT", false, null, "主键"),
                new FieldCandidate("USER_ORDER", "user_name", "VARCHAR(50)", true, null, "用户名")
        ));

        var result = service.importCandidates(req);

        assertThat(result.getImportedCount()).isEqualTo(1);
        assertThat(result.getSkippedCount()).isEqualTo(1);
        verify(sourceService).createDatabaseBatch(req, 1, 1);
        ArgumentCaptor<Field> fieldCaptor = ArgumentCaptor.forClass(Field.class);
        ArgumentCaptor<FieldCandidate> candidateCaptor = ArgumentCaptor.forClass(FieldCandidate.class);
        verify(sourceService).recordFieldSource(eq(batch), fieldCaptor.capture(), candidateCaptor.capture());
        assertThat(fieldCaptor.getValue().getId()).isEqualTo(99L);
        assertThat(candidateCaptor.getValue().getColumnName()).isEqualTo("user_name");
    }

    @Test
    void importCandidates_doesNotCreateBatchWhenAllFieldsAreSkipped() {
        FieldService fieldService = mock(FieldService.class);
        ReverseImportSourceService sourceService = mock(ReverseImportSourceService.class);
        when(fieldService.listByProject(1L)).thenReturn(List.of(standardField("id", null)));
        ReverseImportServiceImpl service = new ReverseImportServiceImpl(
                new SqlParserService(),
                fieldService,
                sourceService);
        DatabaseImportReq req = databaseImportReq(List.of(
                new FieldCandidate("USER_ORDER", "id", "BIGINT", false, null, "主键")
        ));

        var result = service.importCandidates(req);

        assertThat(result.getImportedCount()).isZero();
        assertThat(result.getSkippedCount()).isEqualTo(1);
        verify(sourceService, never()).createDatabaseBatch(any(), anyInt(), anyInt());
        verify(sourceService, never()).recordFieldSource(any(), any(), any());
    }

    @Test
    void importCandidates_recordsBatchForLegacyDatabaseImportRequestsWithoutSourceContext() {
        FieldService fieldService = mock(FieldService.class);
        ReverseImportSourceService sourceService = mock(ReverseImportSourceService.class);
        when(fieldService.listByProject(1L)).thenReturn(List.of());
        when(fieldService.create(any(Field.class))).thenAnswer(invocation -> {
            Field field = invocation.getArgument(0);
            field.setId(100L);
            return field;
        });
        ReverseImportBatch batch = new ReverseImportBatch();
        batch.setId(8L);
        when(sourceService.createDatabaseBatch(any(DatabaseImportReq.class), eq(1), eq(0)))
                .thenReturn(batch);
        ReverseImportServiceImpl service = new ReverseImportServiceImpl(
                new SqlParserService(),
                fieldService,
                sourceService);
        DatabaseImportReq req = new DatabaseImportReq();
        req.setProjectId(1L);
        req.setCandidates(List.of(
                new FieldCandidate("USER_ORDER", "user_name", "VARCHAR(50)", true, null, "用户名")
        ));

        var result = service.importCandidates(req);

        assertThat(result.getImportedCount()).isEqualTo(1);
        verify(sourceService).createDatabaseBatch(req, 1, 0);
        verify(sourceService).recordFieldSource(eq(batch), any(Field.class), any(FieldCandidate.class));
    }

    private DatabaseImportReq databaseImportReq(List<FieldCandidate> candidates) {
        DatabaseImportReq req = new DatabaseImportReq();
        req.setProjectId(1L);
        req.setDatabaseType("postgresql");
        req.setDatabaseName("demo");
        req.setSchemaName("public");
        req.setTableNames(List.of("USER_ORDER"));
        req.setCandidates(candidates);
        return req;
    }

    private ColumnDef column(String name, String dataType, boolean nullable, String defaultValue, String comment) {
        return ColumnDef.builder()
                .name(name)
                .dataType(dataType)
                .nullable(nullable)
                .defaultValue(defaultValue)
                .comment(comment)
                .build();
    }

    private Field standardField(String name, String aliases) {
        Field field = new Field();
        field.setProjectId(1L);
        field.setName(name);
        field.setAliases(aliases);
        return field;
    }
}
