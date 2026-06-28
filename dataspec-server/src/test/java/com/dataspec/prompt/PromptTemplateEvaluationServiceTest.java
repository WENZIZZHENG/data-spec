package com.dataspec.prompt;

import com.dataspec.common.result.R;
import com.dataspec.prompt.controller.PromptTemplateController;
import com.dataspec.prompt.model.PromptTemplateDefinition;
import com.dataspec.prompt.model.PromptTemplateEvalReq;
import com.dataspec.prompt.model.PromptTemplateEvalResult;
import com.dataspec.prompt.service.PromptTemplateEvaluationService;
import com.dataspec.prompt.service.PromptTemplateRegistry;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Prompt 模板 registry 与本地评测测试。
 */
class PromptTemplateEvaluationServiceTest {

    private final PromptTemplateRegistry registry = new PromptTemplateRegistry();
    private final PromptTemplateEvaluationService evaluationService = new PromptTemplateEvaluationService(registry);

    @Test
    void registryListsStableTemplateDefinitions() {
        List<PromptTemplateDefinition> templates = registry.listTemplates();

        assertTrue(templates.size() >= 5);
        PromptTemplateDefinition createTable = registry.getTemplate(PromptTemplateRegistry.CREATE_TABLE);
        assertEquals("create-table-prompt@1", createTable.promptVersion());
        assertEquals("CREATE_TABLE", createTable.scenario());
        assertFalse(createTable.requiredSections().isEmpty());
        assertFalse(createTable.requiredPhrases().isEmpty());
        assertTrue(registry.hasPromptVersion("sql-lint-fix@1"));
        assertTrue(registry.hasPromptVersion("ddl-preview@1"));
        assertTrue(templates.stream()
                .allMatch(template -> template.promptVersion().startsWith(template.templateKey() + "@")));
    }

    @Test
    void evaluateGoldenPromptFixturesPassesWithoutExternalLlm() throws Exception {
        PromptTemplateEvalResult createTable = evaluationService.evaluateAgainstGolden(
                PromptTemplateRegistry.CREATE_TABLE,
                fixture("create-table-prompt-golden.md"),
                fixture("create-table-prompt-golden.md"));
        PromptTemplateEvalResult fixSql = evaluationService.evaluateAgainstGolden(
                PromptTemplateRegistry.FIX_SQL,
                fixture("fix-sql-prompt-golden.md"),
                fixture("fix-sql-prompt-golden.md"));

        assertTrue(createTable.passed());
        assertNull(createTable.diff());
        assertTrue(fixSql.passed());
        assertNull(fixSql.diff());
    }

    @Test
    void evaluateReportsMissingRequiredMarkers() {
        PromptTemplateEvalResult result = evaluationService.evaluate(
                PromptTemplateRegistry.CREATE_TABLE,
                "# DataSpec 建表 Prompt\n\n缺少大部分必备段落");

        assertFalse(result.passed());
        assertTrue(result.failures().stream()
                .anyMatch(failure -> "MISSING_SECTION".equals(failure.kind())));
        assertTrue(result.failures().stream()
                .anyMatch(failure -> "MISSING_PHRASE".equals(failure.kind())));
    }

    @Test
    void evaluateAgainstGoldenReturnsReadableDiff() throws Exception {
        String golden = fixture("create-table-prompt-golden.md");
        String changed = golden.replace("订单模块。", "订单与支付模块。");

        PromptTemplateEvalResult result = evaluationService.evaluateAgainstGolden(
                PromptTemplateRegistry.CREATE_TABLE,
                changed,
                golden);

        assertFalse(result.passed());
        assertNotNull(result.diff());
        assertTrue(result.diff().contains("-订单模块。"));
        assertTrue(result.diff().contains("+订单与支付模块。"));
        assertTrue(result.failures().stream()
                .anyMatch(failure -> "GOLDEN_DIFF".equals(failure.kind())));
    }

    @Test
    void controllerExposesMetadataAndEvaluation() throws Exception {
        PromptTemplateController controller = new PromptTemplateController(registry, evaluationService);

        R<List<PromptTemplateDefinition>> listResponse = controller.listTemplates();
        R<PromptTemplateEvalResult> evalResponse = controller.evaluate(new PromptTemplateEvalReq(
                PromptTemplateRegistry.FIX_SQL,
                fixture("fix-sql-prompt-golden.md")));

        assertEquals(200, listResponse.getCode());
        assertTrue(listResponse.getData().stream()
                .anyMatch(template -> PromptTemplateRegistry.FIX_SQL.equals(template.templateKey())));
        assertEquals(200, evalResponse.getCode());
        assertTrue(evalResponse.getData().passed());
        assertEquals("fix-sql-prompt@1", evalResponse.getData().promptVersion());
    }

    private String fixture(String fileName) throws Exception {
        return Files.readString(Path.of("src/test/resources/fixtures/prompts", fileName), StandardCharsets.UTF_8);
    }
}
