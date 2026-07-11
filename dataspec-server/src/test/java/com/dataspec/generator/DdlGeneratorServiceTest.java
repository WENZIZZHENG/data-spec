package com.dataspec.generator;

import com.dataspec.common.exception.BizException;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.dataspec.aireplay.entity.AiJobRecord;
import com.dataspec.aireplay.model.AiJobRecordCreateReq;
import com.dataspec.aireplay.model.AiJobRecordDetail;
import com.dataspec.aireplay.service.AiJobRecordService;
import com.dataspec.generator.model.DdlGenerateResult;
import com.dataspec.generator.service.DdlGeneratorService;
import com.dataspec.lint.engine.SqlLintService;
import com.dataspec.lint.model.LintResult;
import com.dataspec.prompt.service.PromptTemplateRegistry;
import com.dataspec.standard.dto.StandardSnapshotInfo;
import com.dataspec.standard.service.StandardSnapshotService;
import com.dataspec.template.entity.Template;
import com.dataspec.template.entity.TemplateField;
import com.dataspec.template.service.TemplateService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * DDL 生成服务测试
 */
class DdlGeneratorServiceTest {

    @Test
    void generateFromTemplate_buildsPostgresDdlAndRunsLintSelfCheck() {
        TemplateService templateService = mock(TemplateService.class);
        SqlLintService sqlLintService = mock(SqlLintService.class);
        StandardSnapshotService standardSnapshotService = mock(StandardSnapshotService.class);
        RecordingAiJobRecordService aiJobRecordService = new RecordingAiJobRecordService();
        DdlGeneratorService service = new DdlGeneratorService(templateService, sqlLintService, standardSnapshotService,
                aiJobRecordService, new PromptTemplateRegistry());
        LintResult lintResult = LintResult.of(List.of(), List.of());
        when(standardSnapshotService.getCurrentSnapshot(1L)).thenReturn(snapshotInfo());
        when(templateService.getById(10L)).thenReturn(template(10L, 1L, "订单模板", "用户'订单表"));
        when(templateService.listFields(10L)).thenReturn(List.of(
                field(2L, "user_id", "bigint", false, null, "用户ID", 20),
                field(1L, "order_no", "varchar(32)", false, "'PENDING'", "订单'编号", 10)
        ));
        when(sqlLintService.lint(anyString(), eq(1L))).thenReturn(lintResult);

        DdlGenerateResult result = service.generateFromTemplate(1L, 10L, "user_order");

        assertSame(lintResult, result.lintResult());
        assertEquals("v2026.06.24", result.standardSnapshot().specVersion());
        assertEquals("hash123", result.standardSnapshot().specHash());
        assertNotNull(result.structureSummary());
        String ddl = result.ddl();
        assertTrue(ddl.contains("CREATE TABLE user_order ("));
        assertTrue(ddl.contains("    order_no varchar(32) NOT NULL DEFAULT 'PENDING',"));
        assertTrue(ddl.contains("    user_id bigint NOT NULL"));
        assertTrue(ddl.indexOf("order_no") < ddl.indexOf("user_id"), "字段应按 sortOrder 排序");
        assertTrue(ddl.contains("COMMENT ON TABLE user_order IS '用户''订单表';"));
        assertTrue(ddl.contains("COMMENT ON COLUMN user_order.order_no IS '订单''编号';"));
        assertTrue(result.dialectDiagnostics().stream()
                .anyMatch(item -> "POSTGRESQL_DDL_TARGET".equals(item.code())));
        assertTrue(result.dialectDiagnostics().stream()
                .anyMatch(item -> "MYSQL_DDL_CONVERSION_REQUIRED".equals(item.code())));
        verify(sqlLintService).lint(ddl, 1L);
        assertEquals(1, aiJobRecordService.created.size());
        AiJobRecordCreateReq req = aiJobRecordService.created.get(0);
        assertEquals("DDL_PREVIEW", req.jobType());
        assertEquals("ddl-preview@1", req.promptVersion());
        assertEquals(6L, req.standardSnapshotId());
        assertTrue(req.inputPayload().toString().contains("templateId=10"));
        assertTrue(req.outputPayload().toString().contains("ddl="));
        assertTrue(req.outputPayload().toString().contains("POSTGRESQL_DDL_TARGET"));
    }

    @Test
    void generateFromTemplateAiContract_exposesStableDdlPreviewFields() {
        TemplateService templateService = mock(TemplateService.class);
        SqlLintService sqlLintService = mock(SqlLintService.class);
        StandardSnapshotService standardSnapshotService = mock(StandardSnapshotService.class);
        DdlGeneratorService service = new DdlGeneratorService(templateService, sqlLintService, standardSnapshotService,
                new NoopAiJobRecordService(), new PromptTemplateRegistry());
        when(standardSnapshotService.getCurrentSnapshot(1L)).thenReturn(snapshotInfo());
        when(templateService.getById(10L)).thenReturn(template(10L, 1L, "订单模板", "用户订单表"));
        when(templateService.listFields(10L)).thenReturn(List.of(
                field(1L, "order_no", "varchar(32)", false, null, "订单编号", 10)
        ));
        when(sqlLintService.lint(anyString(), eq(1L))).thenReturn(LintResult.of(List.of(), List.of()));

        DdlGenerateResult result = service.generateFromTemplate(1L, 10L, "user_order");

        JsonNode root = new ObjectMapper().valueToTree(result);
        assertTrue(root.path("ddl").asText().contains("CREATE TABLE user_order"));
        assertEquals(0, root.path("lintResult").path("errorCount").asInt());
        assertTrue(root.path("lintResult").path("issues").isArray());
        assertEquals("v2026.06.24", root.path("standardSnapshot").path("specVersion").asText());
        assertEquals("hash123", root.path("standardSnapshot").path("specHash").asText());
        assertEquals("POSTGRESQL_DDL_TARGET", root.path("dialectDiagnostics").get(0).path("code").asText());
        assertTrue(root.path("structureSummary").path("appliedConstraints").isArray());
    }

    @Test
    void generateFromTemplate_consumesStructuredTableStandards() {
        TemplateService templateService = mock(TemplateService.class);
        SqlLintService sqlLintService = mock(SqlLintService.class);
        StandardSnapshotService standardSnapshotService = mock(StandardSnapshotService.class);
        RecordingAiJobRecordService aiJobRecordService = new RecordingAiJobRecordService();
        DdlGeneratorService service = new DdlGeneratorService(templateService, sqlLintService, standardSnapshotService,
                aiJobRecordService, new PromptTemplateRegistry());
        when(standardSnapshotService.getCurrentSnapshot(1L)).thenReturn(snapshotInfo());
        Template template = template(10L, 1L, "订单模板", "用户订单表");
        template.setBusinessObjectId(99L);
        template.setPrimaryKeyJson("{\"name\":\"user_order_pk\",\"columns\":[\"id\"]}");
        template.setUniqueKeysJson("[{\"name\":\"user_order_order_no_uk\",\"columns\":[\"order_no\"]}]");
        template.setIndexesJson("[{\"name\":\"idx_user_order_user_id\",\"columns\":[\"user_id\"]}]");
        template.setForeignKeysJson("[{\"name\":\"user_order_user_fk\",\"columns\":[\"user_id\"],\"targetTable\":\"user_account\",\"targetColumns\":[\"id\"],\"onDelete\":\"RESTRICT\"}]");
        template.setCheckHintsJson("[\"amount_cent >= 0\"]");
        template.setAiUsageNotes("订单表必须保留审计字段");
        when(templateService.getById(10L)).thenReturn(template);
        when(templateService.listFields(10L)).thenReturn(List.of(
                field(1L, "id", "bigserial", false, null, "主键", 1),
                field(2L, "order_no", "varchar(32)", false, null, "订单编号", 2),
                field(3L, "user_id", "bigint", false, null, "用户ID", 3)
        ));
        when(sqlLintService.lint(anyString(), eq(1L))).thenReturn(LintResult.of(List.of(), List.of()));

        DdlGenerateResult result = service.generateFromTemplate(1L, 10L, "user_order");

        assertTrue(result.ddl().contains("CONSTRAINT user_order_pk PRIMARY KEY (id)"));
        assertTrue(result.ddl().contains("CONSTRAINT user_order_order_no_uk UNIQUE (order_no)"));
        assertTrue(result.ddl().contains("CONSTRAINT user_order_user_fk FOREIGN KEY (user_id) REFERENCES user_account (id) ON DELETE RESTRICT"));
        assertTrue(result.ddl().contains("CREATE INDEX idx_user_order_user_id ON user_order (user_id);"));
        assertTrue(result.structureSummary().appliedConstraints().contains("primaryKey:user_order_pk(id)"));
        assertTrue(result.structureSummary().generatedIndexes().contains("index:idx_user_order_user_id(user_id)"));
        assertTrue(result.structureSummary().policyNotes().contains("checkHint:amount_cent >= 0"));
        assertTrue(result.structureSummary().policyNotes().contains("aiUsageNotes:订单表必须保留审计字段"));
        assertTrue(result.structureSummary().evidence().contains("businessObjectId:99"));
        assertTrue(aiJobRecordService.created.get(0).outputPayload().toString().contains("structureSummary"));
    }

    @Test
    void generateFromTemplate_skipsUnsafeStructureHints() {
        TemplateService templateService = mock(TemplateService.class);
        SqlLintService sqlLintService = mock(SqlLintService.class);
        StandardSnapshotService standardSnapshotService = mock(StandardSnapshotService.class);
        DdlGeneratorService service = new DdlGeneratorService(templateService, sqlLintService, standardSnapshotService,
                new NoopAiJobRecordService(), new PromptTemplateRegistry());
        when(standardSnapshotService.getCurrentSnapshot(1L)).thenReturn(snapshotInfo());
        Template template = template(10L, 1L, "订单模板", "用户订单表");
        template.setIndexesJson("[{\"name\":\"idx_bad\",\"columns\":[\"missing_column\"]},{\"name\":\"idx_hack;drop\",\"columns\":[\"id\"]}]");
        template.setForeignKeysJson("[{\"columns\":[\"id\"],\"targetTable\":\"UserTable\",\"targetColumns\":[\"id\"],\"onDelete\":\"DROP\"}]");
        when(templateService.getById(10L)).thenReturn(template);
        when(templateService.listFields(10L)).thenReturn(List.of(
                field(1L, "id", "bigint", false, null, "主键", 1)
        ));
        when(sqlLintService.lint(anyString(), eq(1L))).thenReturn(LintResult.of(List.of(), List.of()));

        DdlGenerateResult result = service.generateFromTemplate(1L, 10L, "user_order");

        assertFalse(result.ddl().contains("missing_column"));
        assertFalse(result.ddl().contains("idx_hack;drop"));
        assertFalse(result.ddl().contains("UserTable"));
        assertTrue(result.structureSummary().skippedHints().stream()
                .anyMatch(item -> item.contains("模板中不存在的字段")));
        assertTrue(result.structureSummary().skippedHints().stream()
                .anyMatch(item -> item.contains("外键目标表名")));
    }

    @Test
    void generateFromTemplate_redactsSensitiveStructureSummaryInReplay() {
        TemplateService templateService = mock(TemplateService.class);
        SqlLintService sqlLintService = mock(SqlLintService.class);
        StandardSnapshotService standardSnapshotService = mock(StandardSnapshotService.class);
        RecordingAiJobRecordService aiJobRecordService = new RecordingAiJobRecordService();
        DdlGeneratorService service = new DdlGeneratorService(templateService, sqlLintService, standardSnapshotService,
                aiJobRecordService, new PromptTemplateRegistry());
        when(standardSnapshotService.getCurrentSnapshot(1L)).thenReturn(snapshotInfo());
        Template template = template(10L, 1L, "订单模板", "用户订单表");
        template.setCheckHintsJson("[\"token=raw-structure-token\"]");
        template.setAiUsageNotes("password=raw-structure-password");
        when(templateService.getById(10L)).thenReturn(template);
        when(templateService.listFields(10L)).thenReturn(List.of(
                field(1L, "id", "bigint", false, null, "主键", 1)
        ));
        when(sqlLintService.lint(anyString(), eq(1L))).thenReturn(LintResult.of(List.of(), List.of()));

        DdlGenerateResult result = service.generateFromTemplate(1L, 10L, "user_order");

        String summaryText = result.structureSummary().toString();
        String replayText = aiJobRecordService.created.get(0).outputPayload().toString();
        assertFalse(summaryText.contains("raw-structure-token"));
        assertFalse(summaryText.contains("raw-structure-password"));
        assertFalse(replayText.contains("raw-structure-token"));
        assertFalse(replayText.contains("raw-structure-password"));
        assertTrue(summaryText.contains("[REDACTED]"));
        assertTrue(replayText.contains("[REDACTED]"));
    }

    @Test
    void generateFromTemplate_rejectsTemplateFromAnotherProject() {
        TemplateService templateService = mock(TemplateService.class);
        SqlLintService sqlLintService = mock(SqlLintService.class);
        DdlGeneratorService service = new DdlGeneratorService(templateService, sqlLintService, mock(StandardSnapshotService.class),
                new NoopAiJobRecordService(), new PromptTemplateRegistry());
        when(templateService.getById(10L)).thenReturn(template(10L, 2L, "订单模板", "用户订单表"));

        assertThrows(BizException.class, () -> service.generateFromTemplate(1L, 10L, "user_order"));

        verify(templateService, never()).listFields(anyLong());
        verify(sqlLintService, never()).lint(anyString(), any());
    }

    @Test
    void generateFromTemplate_rejectsUnsafeIdentifierBeforeLoadingTemplate() {
        TemplateService templateService = mock(TemplateService.class);
        SqlLintService sqlLintService = mock(SqlLintService.class);
        DdlGeneratorService service = new DdlGeneratorService(templateService, sqlLintService, mock(StandardSnapshotService.class),
                new NoopAiJobRecordService(), new PromptTemplateRegistry());

        assertThrows(BizException.class, () -> service.generateFromTemplate(1L, 10L, "UserOrder"));

        verifyNoInteractions(templateService, sqlLintService);
    }

    @Test
    void generateFromTemplate_normalizesAcceptedSqlFragmentsBeforeRendering() {
        TemplateService templateService = mock(TemplateService.class);
        SqlLintService sqlLintService = mock(SqlLintService.class);
        StandardSnapshotService standardSnapshotService = mock(StandardSnapshotService.class);
        DdlGeneratorService service = new DdlGeneratorService(templateService, sqlLintService, standardSnapshotService,
                new NoopAiJobRecordService(), new PromptTemplateRegistry());
        when(standardSnapshotService.getCurrentSnapshot(1L)).thenReturn(snapshotInfo());
        when(templateService.getById(10L)).thenReturn(template(10L, 1L, "订单模板", "用户订单表"));
        when(templateService.listFields(10L)).thenReturn(List.of(
                field(1L, " order_no ", " varchar(32) ", false, " 'PENDING' ", "订单编号", 10)
        ));
        when(sqlLintService.lint(anyString(), eq(1L))).thenReturn(LintResult.of(List.of(), List.of()));

        DdlGenerateResult result = service.generateFromTemplate(1L, 10L, " user_order ");

        assertTrue(result.ddl().contains("CREATE TABLE user_order ("));
        assertTrue(result.ddl().contains("    order_no varchar(32) NOT NULL DEFAULT 'PENDING'"));
        assertTrue(result.ddl().contains("COMMENT ON COLUMN user_order.order_no IS '订单编号';"));
        verify(sqlLintService).lint(result.ddl(), 1L);
    }

    @Test
    void generateFromTemplate_rejectsTemplateWithoutFields() {
        TemplateService templateService = mock(TemplateService.class);
        SqlLintService sqlLintService = mock(SqlLintService.class);
        DdlGeneratorService service = new DdlGeneratorService(templateService, sqlLintService, mock(StandardSnapshotService.class),
                new NoopAiJobRecordService(), new PromptTemplateRegistry());
        when(templateService.getById(10L)).thenReturn(template(10L, 1L, "订单模板", "用户订单表"));
        when(templateService.listFields(10L)).thenReturn(List.of());

        assertThrows(BizException.class, () -> service.generateFromTemplate(1L, 10L, "user_order"));

        verify(sqlLintService, never()).lint(anyString(), any());
    }

    @Test
    void generateFromTemplate_rejectsDataTypeThatCouldRenderExtraColumns() {
        TemplateService templateService = mock(TemplateService.class);
        SqlLintService sqlLintService = mock(SqlLintService.class);
        DdlGeneratorService service = new DdlGeneratorService(templateService, sqlLintService, mock(StandardSnapshotService.class),
                new NoopAiJobRecordService(), new PromptTemplateRegistry());
        when(templateService.getById(10L)).thenReturn(template(10L, 1L, "订单模板", "用户订单表"));
        when(templateService.listFields(10L)).thenReturn(List.of(
                field(1L, "order_no", "bigint, hacked text", false, null, "订单编号", 10)
        ));

        assertThrows(BizException.class, () -> service.generateFromTemplate(1L, 10L, "user_order"));

        verify(sqlLintService, never()).lint(anyString(), any());
    }

    @Test
    void generateFromTemplate_rejectsDefaultValueThatCouldRenderExtraColumns() {
        TemplateService templateService = mock(TemplateService.class);
        SqlLintService sqlLintService = mock(SqlLintService.class);
        DdlGeneratorService service = new DdlGeneratorService(templateService, sqlLintService, mock(StandardSnapshotService.class),
                new NoopAiJobRecordService(), new PromptTemplateRegistry());
        when(templateService.getById(10L)).thenReturn(template(10L, 1L, "订单模板", "用户订单表"));
        when(templateService.listFields(10L)).thenReturn(List.of(
                field(1L, "order_no", "varchar(32)", false, "'PENDING', hacked text", "订单编号", 10)
        ));

        assertThrows(BizException.class, () -> service.generateFromTemplate(1L, 10L, "user_order"));

        verify(sqlLintService, never()).lint(anyString(), any());
    }

    private Template template(Long id, Long projectId, String name, String description) {
        Template template = new Template();
        template.setId(id);
        template.setProjectId(projectId);
        template.setName(name);
        template.setDescription(description);
        return template;
    }

    private StandardSnapshotInfo snapshotInfo() {
        return new StandardSnapshotInfo(6L, 1L, "v2026.06.24", "P6-1", null, "hash123", null, true);
    }

    private TemplateField field(Long id, String name, String dataType, boolean nullable,
                                String defaultValue, String comment, int sortOrder) {
        TemplateField field = new TemplateField();
        field.setId(id);
        field.setName(name);
        field.setDataType(dataType);
        field.setNullable(nullable);
        field.setDefaultValue(defaultValue);
        field.setComment(comment);
        field.setSortOrder(sortOrder);
        return field;
    }

    private static class NoopAiJobRecordService implements AiJobRecordService {
        @Override
        public AiJobRecord create(AiJobRecordCreateReq req) {
            return new AiJobRecord();
        }

        @Override
        public IPage<AiJobRecord> listByProject(Long projectId, String jobType, int current, int size) {
            throw new UnsupportedOperationException();
        }

        @Override
        public AiJobRecordDetail getDetail(Long id) {
            throw new UnsupportedOperationException();
        }
    }

    private static class RecordingAiJobRecordService extends NoopAiJobRecordService {
        final List<AiJobRecordCreateReq> created = new java.util.ArrayList<>();

        @Override
        public AiJobRecord create(AiJobRecordCreateReq req) {
            created.add(req);
            return new AiJobRecord();
        }
    }
}
