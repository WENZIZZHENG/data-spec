package com.dataspec.aicontext;

import com.dataspec.aicontext.model.AiContextBudgetPlan;
import com.dataspec.aicontext.model.AiContextBudgetPlanRequest;
import com.dataspec.aicontext.model.AiContextBudgetQualityRisk;
import com.dataspec.aicontext.service.AiContextBudgetPlannerService;
import com.dataspec.aiprofile.service.AiTaskProfileService;
import com.dataspec.businessglossary.model.BusinessGlossaryContextExport;
import com.dataspec.businessglossary.service.BusinessGlossaryService;
import com.dataspec.enumdict.service.EnumDictService;
import com.dataspec.field.entity.Field;
import com.dataspec.field.service.FieldService;
import com.dataspec.lint.engine.SqlLintService;
import com.dataspec.rule.entity.RuleConfig;
import com.dataspec.rule.service.RuleConfigService;
import com.dataspec.standardusageexample.entity.StandardUsageExample;
import com.dataspec.standardusageexample.service.StandardUsageExampleService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * AI Context 预算 planner 服务测试。
 */
class AiContextBudgetPlannerServiceTest {

    private static final Long PROJECT_ID = 1L;

    @Test
    void planWithFullBudgetSelectsAllArtifactsAndLowRisk() {
        AiContextBudgetPlannerService service = createService(List.of(sampleField("mobile_no", "手机号")));

        AiContextBudgetPlan plan = service.plan(new AiContextBudgetPlanRequest(
                PROJECT_ID, 20_000, null, null, null, null, null, null, null, null));

        assertEquals("dataspec-ai-context-budget-plan", plan.kind());
        assertEquals(1, plan.schemaVersion());
        assertEquals(PROJECT_ID, plan.projectId());
        assertEquals(AiContextBudgetQualityRisk.LOW, plan.qualityRisk());
        assertTrue(plan.droppedArtifacts().isEmpty());
        assertTrue(plan.selectedArtifacts().stream().anyMatch(item -> ".dataspec/field-catalog.json".equals(item.artifact())));
        assertTrue(plan.estimation().selectedEstimatedTokens() <= plan.estimation().tokenBudget());
        assertEquals("all", plan.recommendedExportParams().scope());
    }

    @Test
    void planWithStandardBudgetKeepsCriticalScopedContextAndMediumRisk() {
        AiContextBudgetPlannerService service = createService(List.of(sampleField("mobile_no", "手机号")));

        AiContextBudgetPlan plan = service.plan(new AiContextBudgetPlanRequest(
                PROJECT_ID, 2_400, null, null, "field", "手机号", "enabled", 20, null, null));

        assertEquals(AiContextBudgetQualityRisk.MEDIUM, plan.qualityRisk());
        assertTrue(plan.selectedArtifacts().stream().anyMatch(item -> ".dataspec/DATABASE_RULES.md".equals(item.artifact())));
        assertTrue(plan.selectedArtifacts().stream().anyMatch(item -> ".dataspec/field-catalog.json".equals(item.artifact())));
        assertTrue(plan.droppedArtifacts().stream().anyMatch(item -> ".dataspec/prompts.md".equals(item.artifact())));
        assertEquals("field", plan.recommendedExportParams().scope());
        assertEquals("手机号", plan.recommendedExportParams().query());
        assertEquals(1, plan.request().matchedFieldCount());
    }

    @Test
    void planWithTinyBudgetDropsCriticalContextAndHighRisk() {
        AiContextBudgetPlannerService service = createService(List.of(sampleField("mobile_no", "手机号")));

        AiContextBudgetPlan plan = service.plan(new AiContextBudgetPlanRequest(
                PROJECT_ID, 300, null, null, "field", "手机号", null, 10, null, null));

        assertEquals(AiContextBudgetQualityRisk.HIGH, plan.qualityRisk());
        assertTrue(plan.droppedArtifacts().stream().anyMatch(item -> ".dataspec/field-catalog.json".equals(item.artifact())));
        assertFalse(plan.fallbackSteps().isEmpty());
        assertTrue(plan.recommendedNextActions().stream().anyMatch(action -> action.contains("提高 tokenBudget")));
    }

    @Test
    void planWithLowBudgetWithoutQueryReportsDiagnostic() {
        AiContextBudgetPlannerService service = createService(List.of(sampleField("mobile_no", "手机号")));

        AiContextBudgetPlan plan = service.plan(new AiContextBudgetPlanRequest(
                PROJECT_ID, 1_200, null, null, null, null, null, null, null, null));

        assertTrue(plan.diagnostics().stream().anyMatch(item -> item.contains("query") || item.contains("检索")));
        assertTrue(plan.recommendedNextActions().stream().anyMatch(action -> action.contains("query")));
    }

    @Test
    void planWithEmptyScopedMatchesUsesFallback() {
        AiContextBudgetPlannerService service = createService(List.of(sampleField("mobile_no", "手机号")));

        AiContextBudgetPlan plan = service.plan(new AiContextBudgetPlanRequest(
                PROJECT_ID, 2_400, null, null, "field", "不存在的字段", null, 10, null, null));

        assertTrue(plan.qualityRisk().ordinal() >= AiContextBudgetQualityRisk.MEDIUM.ordinal());
        assertEquals(0, plan.request().matchedFieldCount());
        assertTrue(plan.diagnostics().stream().anyMatch(item -> item.contains("未命中")));
        assertTrue(plan.fallbackSteps().stream().anyMatch(step -> step.contains("放宽 query")));
    }

    @Test
    void planRedactsSensitiveRequestText() throws Exception {
        AiContextBudgetPlannerService service = createService(List.of(sampleField("mobile_no", "手机号")));

        AiContextBudgetPlan plan = service.plan(new AiContextBudgetPlanRequest(
                PROJECT_ID,
                2_400,
                null,
                null,
                "field",
                "Authorization: Bearer abc123 token=xyz password=123 jdbc:postgresql://localhost/db",
                null,
                10,
                "dsn=postgresql://localhost/app",
                "api_key=unsafe"));
        String json = new ObjectMapper().writeValueAsString(plan);

        assertFalse(json.contains("abc123"));
        assertFalse(json.contains("xyz"));
        assertFalse(json.contains("password=123"));
        assertFalse(json.contains("jdbc:postgresql://localhost/db"));
        assertFalse(json.contains("postgresql://localhost/app"));
        assertFalse(json.contains("api_key=unsafe"));
        assertTrue(json.contains("[REDACTED]"));
    }

    private AiContextBudgetPlannerService createService(List<Field> fields) {
        FieldService fieldService = mock(FieldService.class);
        RuleConfigService ruleConfigService = mock(RuleConfigService.class);
        EnumDictService enumDictService = mock(EnumDictService.class);
        SqlLintService sqlLintService = mock(SqlLintService.class);
        AiTaskProfileService aiTaskProfileService = mock(AiTaskProfileService.class);
        BusinessGlossaryService businessGlossaryService = mock(BusinessGlossaryService.class);
        StandardUsageExampleService usageExampleService = mock(StandardUsageExampleService.class);

        when(fieldService.listByProject(PROJECT_ID)).thenReturn(fields);
        when(ruleConfigService.listEnabledByProject(PROJECT_ID)).thenReturn(List.of(rule("table_naming_snake_case")));
        when(enumDictService.listByProject(PROJECT_ID)).thenReturn(List.of());
        when(sqlLintService.listAvailableRules()).thenReturn(List.of(Map.of("code", "table_naming_snake_case", "name", "表名蛇形命名")));
        when(businessGlossaryService.contextExport(eq(PROJECT_ID), anyInt())).thenReturn(BusinessGlossaryContextExport.empty());
        when(usageExampleService.selectForAiContext(eq(PROJECT_ID), anyList(), any(), anyInt()))
                .thenReturn(List.of(usageExample(1L)));

        return new AiContextBudgetPlannerService(
                fieldService,
                ruleConfigService,
                enumDictService,
                sqlLintService,
                aiTaskProfileService,
                businessGlossaryService,
                usageExampleService);
    }

    private Field sampleField(String name, String displayName) {
        Field field = new Field();
        field.setId(10L);
        field.setName(name);
        field.setDisplayName(displayName);
        field.setDataType("varchar(20)");
        field.setComment(displayName);
        field.setCategory("contact");
        field.setTags("pii,customer");
        field.setAliases("phone,mobile");
        field.setStatus("enabled");
        field.setSensitive(true);
        return field;
    }

    private RuleConfig rule(String code) {
        RuleConfig rule = new RuleConfig();
        rule.setRuleCode(code);
        rule.setRuleName("表名蛇形命名");
        rule.setSeverity("ERROR");
        rule.setEnabled(true);
        return rule;
    }

    private StandardUsageExample usageExample(Long id) {
        StandardUsageExample example = new StandardUsageExample();
        example.setId(id);
        example.setProjectId(PROJECT_ID);
        example.setFieldId(10L);
        example.setScope("FIELD");
        example.setExampleType("GOOD");
        example.setInput("手机号字段");
        example.setExpectedOutput("mobile_no varchar(20)");
        example.setStatus("enabled");
        return example;
    }
}
