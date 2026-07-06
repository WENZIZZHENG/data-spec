package com.dataspec.aitaskrecommendation;

import com.dataspec.aitaskrecommendation.controller.AiTaskRecommendationController;
import com.dataspec.aitaskrecommendation.model.AiTaskRecommendationItem;
import com.dataspec.aitaskrecommendation.model.AiTaskRecommendationReport;
import com.dataspec.aitaskrecommendation.model.AiTaskRecommendationSummary;
import com.dataspec.aitaskrecommendation.service.AiTaskRecommendationService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class AiTaskRecommendationControllerTest {

    @Test
    void report_returnsWrappedRecommendationQueue() throws Exception {
        AiTaskRecommendationService service = mock(AiTaskRecommendationService.class);
        when(service.report(1L)).thenReturn(new AiTaskRecommendationReport(
                1L,
                new AiTaskRecommendationSummary(1, 1, 0, 0, 2),
                List.of(new AiTaskRecommendationItem(
                        "FIX_QUALITY_GATE",
                        "HIGH",
                        "修复质量门禁失败项",
                        "当前有 2 个质量门禁失败项。",
                        "/quality-gate",
                        "POST /api/quality-gate/evaluate {\"projectId\":1}",
                        List.of("qualityGate.failedChecks=2"),
                        "failedChecks 降为 0"
                ))));
        MockMvc mockMvc = standaloneSetup(new AiTaskRecommendationController(service)).build();

        mockMvc.perform(get("/api/ai-task-recommendations").param("projectId", "1"))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.projectId").value(1))
                .andExpect(jsonPath("$.data.summary.totalTaskCount").value(1))
                .andExpect(jsonPath("$.data.items[0].taskType").value("FIX_QUALITY_GATE"))
                .andExpect(jsonPath("$.data.items[0].priority").value("HIGH"));
    }
}
