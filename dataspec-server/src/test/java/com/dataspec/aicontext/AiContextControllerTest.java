package com.dataspec.aicontext;

import com.dataspec.aicontext.controller.AiContextController;
import com.dataspec.aicontext.model.AiContextBudgetEstimation;
import com.dataspec.aicontext.model.AiContextBudgetPlan;
import com.dataspec.aicontext.model.AiContextBudgetPlanRequest;
import com.dataspec.aicontext.model.AiContextBudgetQualityRisk;
import com.dataspec.aicontext.model.AiContextBudgetRequestEcho;
import com.dataspec.aicontext.model.AiContextRecommendedExportParams;
import com.dataspec.aicontext.model.AiContextScopeOptions;
import com.dataspec.aicontext.service.AiContextBudgetPlannerService;
import com.dataspec.aicontext.service.AiContextExportService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * AI Context 导出接口测试
 */
class AiContextControllerTest {

    @Test
    void downloadAiContextPackage_returnsZipAttachment() {
        AiContextExportService service = mock(AiContextExportService.class);
        when(service.generateAiContextPackage(eq(1L), eq(AiContextScopeOptions.full()), eq(null), eq(null))).thenReturn(new byte[] { 1, 2, 3 });
        AiContextController controller = controller(service);

        var response = controller.downloadAiContextPackage(1L, null, null, null, null, null, null, null, null);

        assertEquals(MediaType.parseMediaType("application/zip"), response.getHeaders().getContentType());
        assertEquals("attachment; filename=dataspec-ai-context.zip",
                response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION));
        assertArrayEquals(new byte[] { 1, 2, 3 }, response.getBody());
    }

    @Test
    void previewFieldCatalog_forwardsScopeOptions() {
        AiContextExportService service = mock(AiContextExportService.class);
        AiContextScopeOptions options = new AiContextScopeOptions("field", "手机", "enabled", 20);
        when(service.generateFieldCatalogJson(1L, options, null, null)).thenReturn("{\"fields\":[]}");
        AiContextController controller = controller(service);

        var response = controller.previewFieldCatalog(1L, "field", "手机", "enabled", 20, null, null, null, null);

        assertEquals("{\"fields\":[]}", response.getData());
        verify(service).generateFieldCatalogJson(1L, options, null, null);
    }

    @Test
    void previewDatabaseRules_forwardsSnapshotOptions() {
        AiContextExportService service = mock(AiContextExportService.class);
        AiContextScopeOptions options = AiContextScopeOptions.full();
        when(service.generateDatabaseRules(1L, options, 42L, "v-history")).thenReturn("# rules");
        AiContextController controller = controller(service);

        var response = controller.previewDatabaseRules(1L, null, null, null, null, null, null, 42L, "v-history");

        assertEquals("# rules", response.getData());
        verify(service).generateDatabaseRules(1L, options, 42L, "v-history");
    }

    @Test
    void previewFieldCatalog_forwardsProfileOptions() {
        AiContextExportService service = mock(AiContextExportService.class);
        AiContextScopeOptions options = new AiContextScopeOptions(null, null, null, null, "minimal-context", "MINIMAL_CONTEXT");
        when(service.generateFieldCatalogJson(1L, options, null, null)).thenReturn("{\"fields\":[]}");
        AiContextController controller = controller(service);

        var response = controller.previewFieldCatalog(1L, null, null, null, null, "minimal-context", "MINIMAL_CONTEXT", null, null);

        assertEquals("{\"fields\":[]}", response.getData());
        verify(service).generateFieldCatalogJson(1L, options, null, null);
    }

    @Test
    void generateCreateTablePrompt_returnsText() {
        AiContextExportService service = mock(AiContextExportService.class);
        when(service.generateCreateTablePrompt(1L, "订单模块")).thenReturn("prompt text");
        AiContextController controller = controller(service);

        var response = controller.generateCreateTablePrompt(
                new AiContextController.CreateTablePromptReq(1L, "订单模块"));

        assertEquals("prompt text", response.getData());
    }

    @Test
    void generateFixSqlPrompt_returnsText() {
        AiContextExportService service = mock(AiContextExportService.class);
        when(service.generateFixSqlPrompt(1L, "select 1")).thenReturn("fix prompt");
        AiContextController controller = controller(service);

        var response = controller.generateFixSqlPrompt(
                new AiContextController.FixSqlPromptReq(1L, "select 1"));

        assertEquals("fix prompt", response.getData());
    }

    @Test
    void planBudget_forwardsBudgetRequest() {
        AiContextExportService exportService = mock(AiContextExportService.class);
        AiContextBudgetPlannerService plannerService = mock(AiContextBudgetPlannerService.class);
        AiContextBudgetPlanRequest request = new AiContextBudgetPlanRequest(
                1L, 2400, "CREATE_TABLE", "standard-context", "field", "手机号", "enabled", 20, null, null);
        AiContextBudgetPlan plan = samplePlan(request);
        when(plannerService.plan(request)).thenReturn(plan);
        AiContextController controller = new AiContextController(exportService, plannerService);

        var response = controller.planBudget(request);

        assertSame(plan, response.getData());
        verify(plannerService).plan(request);
    }

    private AiContextController controller(AiContextExportService service) {
        return new AiContextController(service, mock(AiContextBudgetPlannerService.class));
    }

    private AiContextBudgetPlan samplePlan(AiContextBudgetPlanRequest request) {
        AiContextRecommendedExportParams params = new AiContextRecommendedExportParams(
                "field", "手机号", "enabled", 20, "standard-context", "CREATE_TABLE");
        return new AiContextBudgetPlan(
                "dataspec-ai-context-budget-plan",
                1,
                request.projectId(),
                new AiContextBudgetRequestEcho(
                        request.projectId(),
                        request.tokenBudget(),
                        request.taskType(),
                        request.profileId(),
                        request.scope(),
                        request.query(),
                        request.status(),
                        request.limit(),
                        request.targetTable(),
                        request.targetFile(),
                        1,
                        1,
                        1),
                new AiContextBudgetEstimation(request.tokenBudget(), 1200, 1600, "deterministic-local-character-weight-v1", "conservative"),
                List.of(),
                List.of(),
                AiContextBudgetQualityRisk.MEDIUM,
                List.of(),
                params,
                List.of(),
                List.of());
    }
}
