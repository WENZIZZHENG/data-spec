package com.dataspec.standardusageheatmap;

import com.dataspec.standardusageheatmap.controller.StandardUsageHeatmapController;
import com.dataspec.standardusageheatmap.model.StandardUsageHeatmapItem;
import com.dataspec.standardusageheatmap.model.StandardUsageHeatmapReport;
import com.dataspec.standardusageheatmap.model.StandardUsageHeatmapSummary;
import com.dataspec.standardusageheatmap.service.StandardUsageHeatmapService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class StandardUsageHeatmapControllerTest {

    @Test
    void report_httpRouteBindsProjectIdAndReturnsWrappedJson() throws Exception {
        StandardUsageHeatmapService service = mock(StandardUsageHeatmapService.class);
        LocalDateTime referencedAt = LocalDateTime.of(2026, 7, 7, 10, 0);
        StandardUsageHeatmapReport report = new StandardUsageHeatmapReport(
                1L,
                new StandardUsageHeatmapSummary(1, 1, 1, 1, 1, 80),
                List.of(new StandardUsageHeatmapItem(
                        10L,
                        "mobile_no",
                        "手机号",
                        "enabled",
                        List.of("database"),
                        45,
                        "POOR",
                        1,
                        1,
                        1,
                        1,
                        referencedAt,
                        85,
                        90,
                        "优先修复字段质量和冲突。")));
        when(service.report(1L)).thenReturn(report);
        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new StandardUsageHeatmapController(service))
                .build();

        mockMvc.perform(get("/api/standard-usage/heatmap").param("projectId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.projectId").value(1))
                .andExpect(jsonPath("$.data.summary.hotFieldCount").value(1))
                .andExpect(jsonPath("$.data.items[0].fieldId").value(10))
                .andExpect(jsonPath("$.data.items[0].sourceKinds[0]").value("database"))
                .andExpect(jsonPath("$.data.items[0].lintHits").value(1))
                .andExpect(jsonPath("$.data.items[0].aiJobHits").value(1))
                .andExpect(jsonPath("$.data.items[0].cleanupPriority").value(90));

        verify(service).report(1L);
    }
}
