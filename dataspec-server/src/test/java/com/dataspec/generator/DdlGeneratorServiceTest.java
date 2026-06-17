package com.dataspec.generator;

import com.dataspec.common.exception.BizException;
import com.dataspec.generator.model.DdlGenerateResult;
import com.dataspec.generator.service.DdlGeneratorService;
import com.dataspec.lint.engine.SqlLintService;
import com.dataspec.lint.model.LintResult;
import com.dataspec.template.entity.Template;
import com.dataspec.template.entity.TemplateField;
import com.dataspec.template.service.TemplateService;
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
        DdlGeneratorService service = new DdlGeneratorService(templateService, sqlLintService);
        LintResult lintResult = LintResult.of(List.of(), List.of());
        when(templateService.getById(10L)).thenReturn(template(10L, 1L, "订单模板", "用户'订单表"));
        when(templateService.listFields(10L)).thenReturn(List.of(
                field(2L, "user_id", "bigint", false, null, "用户ID", 20),
                field(1L, "order_no", "varchar(32)", false, "'PENDING'", "订单'编号", 10)
        ));
        when(sqlLintService.lint(anyString(), eq(1L))).thenReturn(lintResult);

        DdlGenerateResult result = service.generateFromTemplate(1L, 10L, "user_order");

        assertSame(lintResult, result.lintResult());
        String ddl = result.ddl();
        assertTrue(ddl.contains("CREATE TABLE user_order ("));
        assertTrue(ddl.contains("    order_no varchar(32) NOT NULL DEFAULT 'PENDING',"));
        assertTrue(ddl.contains("    user_id bigint NOT NULL"));
        assertTrue(ddl.indexOf("order_no") < ddl.indexOf("user_id"), "字段应按 sortOrder 排序");
        assertTrue(ddl.contains("COMMENT ON TABLE user_order IS '用户''订单表';"));
        assertTrue(ddl.contains("COMMENT ON COLUMN user_order.order_no IS '订单''编号';"));
        verify(sqlLintService).lint(ddl, 1L);
    }

    @Test
    void generateFromTemplate_rejectsTemplateFromAnotherProject() {
        TemplateService templateService = mock(TemplateService.class);
        SqlLintService sqlLintService = mock(SqlLintService.class);
        DdlGeneratorService service = new DdlGeneratorService(templateService, sqlLintService);
        when(templateService.getById(10L)).thenReturn(template(10L, 2L, "订单模板", "用户订单表"));

        assertThrows(BizException.class, () -> service.generateFromTemplate(1L, 10L, "user_order"));

        verify(templateService, never()).listFields(anyLong());
        verify(sqlLintService, never()).lint(anyString(), any());
    }

    @Test
    void generateFromTemplate_rejectsUnsafeIdentifierBeforeLoadingTemplate() {
        TemplateService templateService = mock(TemplateService.class);
        SqlLintService sqlLintService = mock(SqlLintService.class);
        DdlGeneratorService service = new DdlGeneratorService(templateService, sqlLintService);

        assertThrows(BizException.class, () -> service.generateFromTemplate(1L, 10L, "UserOrder"));

        verifyNoInteractions(templateService, sqlLintService);
    }

    @Test
    void generateFromTemplate_normalizesAcceptedSqlFragmentsBeforeRendering() {
        TemplateService templateService = mock(TemplateService.class);
        SqlLintService sqlLintService = mock(SqlLintService.class);
        DdlGeneratorService service = new DdlGeneratorService(templateService, sqlLintService);
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
        DdlGeneratorService service = new DdlGeneratorService(templateService, sqlLintService);
        when(templateService.getById(10L)).thenReturn(template(10L, 1L, "订单模板", "用户订单表"));
        when(templateService.listFields(10L)).thenReturn(List.of());

        assertThrows(BizException.class, () -> service.generateFromTemplate(1L, 10L, "user_order"));

        verify(sqlLintService, never()).lint(anyString(), any());
    }

    @Test
    void generateFromTemplate_rejectsDataTypeThatCouldRenderExtraColumns() {
        TemplateService templateService = mock(TemplateService.class);
        SqlLintService sqlLintService = mock(SqlLintService.class);
        DdlGeneratorService service = new DdlGeneratorService(templateService, sqlLintService);
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
        DdlGeneratorService service = new DdlGeneratorService(templateService, sqlLintService);
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
}
