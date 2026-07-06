package com.dataspec.syntheticexample;

import com.dataspec.common.exception.BizException;
import com.dataspec.field.entity.Field;
import com.dataspec.field.service.FieldService;
import com.dataspec.syntheticexample.model.SyntheticStandardExamplePackage;
import com.dataspec.syntheticexample.service.impl.SyntheticStandardExampleServiceImpl;
import com.dataspec.template.entity.Template;
import com.dataspec.template.entity.TemplateField;
import com.dataspec.template.service.TemplateService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 合成标准样例生成服务测试。
 */
class SyntheticStandardExampleServiceImplTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void generate_buildsDeterministicPackageWithAllScenarioArtifacts() throws Exception {
        FieldService fieldService = mock(FieldService.class);
        TemplateService templateService = mock(TemplateService.class);
        when(fieldService.listByProject(1L)).thenReturn(List.of(
                field(1L, "order_id", "bigint", "订单ID", "订单主键", "order"),
                field(2L, "user_id", "bigint", "用户ID", "password=raw-secret", "user"),
                field(3L, "order_status", "varchar(20)", "订单状态", "状态枚举", "status")
        ));
        when(templateService.listByProject(1L)).thenReturn(List.of(template(10L, "订单模板", "用户订单表")));
        when(templateService.listFields(10L)).thenReturn(List.of(
                templateField(100L, "order_id", "bigint", false, "订单ID"),
                templateField(101L, "order_status", "varchar(20)", false, "订单状态")
        ));
        SyntheticStandardExampleServiceImpl service = new SyntheticStandardExampleServiceImpl(fieldService, templateService);

        SyntheticStandardExamplePackage first = service.generate(1L, "order", 6);
        SyntheticStandardExamplePackage second = service.generate(1L, "order", 6);

        assertEquals("dataspec.synthetic-standard-examples", first.kind());
        assertEquals(1, first.schemaVersion());
        assertEquals(1L, first.projectId());
        assertEquals("order", first.scenario());
        assertEquals(first.specHash(), second.specHash());
        assertFalse(first.goodSql().isEmpty());
        assertFalse(first.badSql().isEmpty());
        assertFalse(first.ddlPreviewInputs().isEmpty());
        assertFalse(first.fieldSuggestionQuestions().isEmpty());
        assertFalse(first.standardQaCases().isEmpty());
        assertFalse(first.expectedDiagnostics().isEmpty());
        assertTrue(first.expectedDiagnostics().stream()
                .anyMatch(item -> first.badSql().getFirst().expectedDiagnosticIds().contains(item.id())));
        assertTrue(first.safety().readOnly());
        assertFalse(first.safety().writesProject());
        assertFalse(first.safety().containsRealBusinessRows());
        assertFalse(first.safety().externalLlmUsed());
        assertEquals(3, first.sourceSummary().standardFieldCount());
        assertEquals(1, first.sourceSummary().templateCount());

        String json = objectMapper.writeValueAsString(first);
        assertFalse(json.contains("raw-secret"));
        assertTrue(json.contains("[REDACTED]"));
    }

    @Test
    void generate_changesSpecHashWhenStandardSummaryChanges() {
        FieldService fieldService = mock(FieldService.class);
        TemplateService templateService = mock(TemplateService.class);
        when(fieldService.listByProject(1L))
                .thenReturn(List.of(field(1L, "user_id", "bigint", "用户ID", "用户主键", "user")))
                .thenReturn(List.of(
                        field(1L, "user_id", "bigint", "用户ID", "用户主键", "user"),
                        field(2L, "mobile_no", "varchar(20)", "手机号", "联系方式", "contact")
                ));
        when(templateService.listByProject(1L)).thenReturn(List.of());
        SyntheticStandardExampleServiceImpl service = new SyntheticStandardExampleServiceImpl(fieldService, templateService);

        SyntheticStandardExamplePackage first = service.generate(1L, "user", 5);
        SyntheticStandardExamplePackage second = service.generate(1L, "user", 5);

        assertNotEquals(first.specHash(), second.specHash());
    }

    @Test
    void generate_changesSpecHashWhenCodeSetReferenceChanges() {
        FieldService fieldService = mock(FieldService.class);
        TemplateService templateService = mock(TemplateService.class);
        Field withoutCodeSet = field(1L, "order_status", "varchar(20)", "订单状态", "状态枚举", "status");
        Field withCodeSet = field(1L, "order_status", "varchar(20)", "订单状态", "状态枚举", "status");
        withCodeSet.setCodeSetId(10L);
        when(fieldService.listByProject(1L))
                .thenReturn(List.of(withoutCodeSet))
                .thenReturn(List.of(withCodeSet));
        when(templateService.listByProject(1L)).thenReturn(List.of());
        SyntheticStandardExampleServiceImpl service = new SyntheticStandardExampleServiceImpl(fieldService, templateService);

        SyntheticStandardExamplePackage first = service.generate(1L, "order", 5);
        SyntheticStandardExamplePackage second = service.generate(1L, "order", 5);

        assertEquals(0, first.sourceSummary().codeSetReferenceCount());
        assertEquals(1, second.sourceSummary().codeSetReferenceCount());
        assertNotEquals(first.specHash(), second.specHash());
    }

    @Test
    void generate_allSupportedScenariosUseSafeFallbackWhenMetadataIsSparse() {
        FieldService fieldService = mock(FieldService.class);
        TemplateService templateService = mock(TemplateService.class);
        when(fieldService.listByProject(1L)).thenReturn(List.of());
        when(templateService.listByProject(1L)).thenReturn(List.of());
        SyntheticStandardExampleServiceImpl service = new SyntheticStandardExampleServiceImpl(fieldService, templateService);

        for (String scenario : List.of("user", "order", "payment", "audit")) {
            SyntheticStandardExamplePackage result = service.generate(1L, scenario, 4);

            assertEquals(scenario, result.scenario());
            assertTrue(result.sourceSummary().fallbackUsed());
            assertTrue(result.diagnostics().stream()
                    .anyMatch(item -> "SYNTHETIC_FALLBACK_USED".equals(item.code())));
            assertFalse(result.goodSql().isEmpty());
            assertFalse(result.badSql().isEmpty());
        }
    }

    @Test
    void generate_rejectsUnsupportedScenarioWithReadableDiagnostic() {
        SyntheticStandardExampleServiceImpl service = new SyntheticStandardExampleServiceImpl(
                mock(FieldService.class),
                mock(TemplateService.class));

        BizException ex = assertThrows(BizException.class, () -> service.generate(1L, "invoice", 5));

        assertTrue(ex.getMessage().contains("user"));
        assertTrue(ex.getMessage().contains("order"));
        assertTrue(ex.getMessage().contains("payment"));
        assertTrue(ex.getMessage().contains("audit"));
    }

    private Field field(Long id, String name, String dataType, String displayName, String comment, String category) {
        Field field = new Field();
        field.setId(id);
        field.setProjectId(1L);
        field.setName(name);
        field.setDataType(dataType);
        field.setDisplayName(displayName);
        field.setComment(comment);
        field.setCategory(category);
        field.setStatus("enabled");
        return field;
    }

    private Template template(Long id, String name, String description) {
        Template template = new Template();
        template.setId(id);
        template.setProjectId(1L);
        template.setName(name);
        template.setDescription(description);
        return template;
    }

    private TemplateField templateField(Long id, String name, String dataType, boolean nullable, String comment) {
        TemplateField field = new TemplateField();
        field.setId(id);
        field.setTemplateId(10L);
        field.setName(name);
        field.setDataType(dataType);
        field.setNullable(nullable);
        field.setComment(comment);
        field.setSortOrder(id.intValue());
        return field;
    }
}
