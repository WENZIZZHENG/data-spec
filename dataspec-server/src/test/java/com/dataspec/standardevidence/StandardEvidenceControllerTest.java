package com.dataspec.standardevidence;

import com.dataspec.common.exception.BizException;
import com.dataspec.common.exception.GlobalExceptionHandler;
import com.dataspec.standardevidence.controller.StandardEvidenceController;
import com.dataspec.standardevidence.model.StandardEvidenceItem;
import com.dataspec.standardevidence.model.StandardEvidenceReport;
import com.dataspec.standardevidence.model.StandardEvidenceSubject;
import com.dataspec.standardevidence.model.StandardEvidenceSummary;
import com.dataspec.standardevidence.service.StandardEvidenceService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class StandardEvidenceControllerTest {

    @Test
    void report_httpRouteBindsSubjectAndReturnsWrappedJson() throws Exception {
        StandardEvidenceService service = mock(StandardEvidenceService.class);
        LocalDateTime now = LocalDateTime.of(2026, 7, 7, 12, 0);
        when(service.report(1L, "FIELD", 10L)).thenReturn(new StandardEvidenceReport(
                1L,
                new StandardEvidenceSubject("FIELD", 10L, "mobile_no", "手机号", "varchar(20)", "enabled"),
                new StandardEvidenceSummary(2, "REVIEW", 72, 80, 2, 1, 1, 0, now, true),
                List.of(new StandardEvidenceItem(
                        "PROVENANCE_CONFIDENCE",
                        "field:10",
                        "来源可信度",
                        "字段来源可信度为 REVIEW，建议生成前复核。",
                        "database",
                        "REVIEW",
                        72,
                        now,
                        List.of("confidence.level=REVIEW"))),
                "字段 mobile_no 当前可信度 REVIEW，SQL 检查命中 2 次，AI 作业命中 1 次。",
                List.of("缺少变更日志证据")));
        MockMvc mockMvc = standaloneSetup(new StandardEvidenceController(service)).build();

        mockMvc.perform(get("/api/standard-evidence")
                        .param("projectId", "1")
                        .param("subjectType", "FIELD")
                        .param("subjectId", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.projectId").value(1))
                .andExpect(jsonPath("$.data.subject.subjectType").value("FIELD"))
                .andExpect(jsonPath("$.data.subject.subjectId").value(10))
                .andExpect(jsonPath("$.data.summary.confidenceLevel").value("REVIEW"))
                .andExpect(jsonPath("$.data.items[0].evidenceType").value("PROVENANCE_CONFIDENCE"))
                .andExpect(jsonPath("$.data.aiEvidenceSummary").value("字段 mobile_no 当前可信度 REVIEW，SQL 检查命中 2 次，AI 作业命中 1 次。"))
                .andExpect(jsonPath("$.data.coverageNotes[0]").value("缺少变更日志证据"));

        verify(service).report(1L, "FIELD", 10L);
    }

    @Test
    void report_returnsBusinessErrorForUnsupportedSubjectType() throws Exception {
        StandardEvidenceService service = mock(StandardEvidenceService.class);
        when(service.report(1L, "TABLE", 10L)).thenThrow(new BizException("跨来源证据视图第一版仅支持 FIELD"));
        MockMvc mockMvc = standaloneSetup(new StandardEvidenceController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        mockMvc.perform(get("/api/standard-evidence")
                        .param("projectId", "1")
                        .param("subjectType", "TABLE")
                        .param("subjectId", "10"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("跨来源证据视图第一版仅支持 FIELD"));
    }
}
