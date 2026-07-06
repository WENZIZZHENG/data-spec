package com.dataspec.fieldprovenance;

import com.dataspec.fieldprovenance.controller.FieldProvenanceConfidenceController;
import com.dataspec.fieldprovenance.model.FieldProvenanceConfidenceItem;
import com.dataspec.fieldprovenance.model.FieldProvenanceConfidenceLevel;
import com.dataspec.fieldprovenance.model.FieldProvenanceConfidenceReport;
import com.dataspec.fieldprovenance.model.FieldProvenanceConfidenceSummary;
import com.dataspec.fieldprovenance.service.FieldProvenanceConfidenceService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class FieldProvenanceConfidenceControllerTest {

    @Test
    void report_returnsServiceResult() {
        FieldProvenanceConfidenceService service = mock(FieldProvenanceConfidenceService.class);
        FieldProvenanceConfidenceReport report = new FieldProvenanceConfidenceReport(
                1L,
                new FieldProvenanceConfidenceSummary(0, 0, 0, 0, 0, 0, 0, 0),
                List.of());
        when(service.report(1L)).thenReturn(report);
        FieldProvenanceConfidenceController controller = new FieldProvenanceConfidenceController(service);

        var response = controller.report(1L);

        assertSame(report, response.getData());
        verify(service).report(1L);
    }

    @Test
    void report_httpRouteBindsProjectIdAndReturnsWrappedJson() throws Exception {
        FieldProvenanceConfidenceService service = mock(FieldProvenanceConfidenceService.class);
        FieldProvenanceConfidenceReport report = new FieldProvenanceConfidenceReport(
                1L,
                new FieldProvenanceConfidenceSummary(1, 1, 0, 0, 0, 1, 1, 0),
                List.of(new FieldProvenanceConfidenceItem(
                        10L,
                        "mobile_no",
                        "手机号",
                        "enabled",
                        "database",
                        List.of("database:public.users.mobile_no"),
                        1,
                        1,
                        2,
                        95,
                        "GOOD",
                        90,
                        FieldProvenanceConfidenceLevel.VERIFIED,
                        "可作为 AI 首选标准字段，生成 SQL 或数据模型时优先采用。",
                        List.of())));
        when(service.report(1L)).thenReturn(report);
        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new FieldProvenanceConfidenceController(service))
                .build();

        mockMvc.perform(get("/api/fields/provenance-confidence").param("projectId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.projectId").value(1))
                .andExpect(jsonPath("$.data.summary.verifiedCount").value(1))
                .andExpect(jsonPath("$.data.fields[0].fieldId").value(10))
                .andExpect(jsonPath("$.data.fields[0].confidenceLevel").value("VERIFIED"))
                .andExpect(jsonPath("$.data.fields[0].sourceRefs[0]").value("database:public.users.mobile_no"));

        verify(service).report(1L);
    }
}
