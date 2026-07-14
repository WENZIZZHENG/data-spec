package com.dataspec.aioutputcheck;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.dataspec.aioutputcheck.controller.AiOutputPostCheckController;
import com.dataspec.aioutputcheck.model.AiOutputContentType;
import com.dataspec.aioutputcheck.model.AiOutputPostCheckRequest;
import com.dataspec.aioutputcheck.model.AiOutputPostCheckResult;
import com.dataspec.aioutputcheck.model.AiOutputPostCheckStatus;
import com.dataspec.aioutputcheck.model.AiOutputPostCheckSummary;
import com.dataspec.aioutputcheck.service.AiOutputPostCheckService;
import com.dataspec.reviewfinding.model.ReviewFindingSource;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AiOutputPostCheckControllerTest {

    @Test
    void requestJsonAcceptsExternalTextContentType() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();

        AiOutputPostCheckRequest request = objectMapper.readValue("""
                {
                  "projectId": 1,
                  "contentType": "TEXT",
                  "content": "生成结果引用 field:1:10。"
                }
                """, AiOutputPostCheckRequest.class);

        assertEquals("TEXT", request.contentType().name());
    }

    @Test
    void requestJsonAcceptsAdditiveStructuredFindings() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();

        AiOutputPostCheckRequest request = objectMapper.readValue("""
                {
                  "projectId": 1,
                  "contentType": "TEXT",
                  "content": "评审完成。",
                  "findings": [{
                    "source": "EXTERNAL_AI",
                    "code": "AI_REVIEW_RULE",
                    "severity": "WARNING",
                    "subject": {"projectId": 1, "kind": "AI_OUTPUT", "name": "review"},
                    "evidenceRefs": []
                  }]
                }
                """, AiOutputPostCheckRequest.class);

        assertEquals(1, request.findings().size());
        assertEquals(ReviewFindingSource.EXTERNAL_AI, request.findings().getFirst().source());
    }

    @Test
    void nullFindingElementReturnsBadRequestBeforeServiceCall() throws Exception {
        AiOutputPostCheckService service = mock(AiOutputPostCheckService.class);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new AiOutputPostCheckController(service)).build();

        mockMvc.perform(post("/api/ai-output/check")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"projectId":1,"contentType":"TEXT","content":"review","findings":[null]}
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    void supplementaryCharactersUseCodePointLimitsAtHttpBoundary() throws Exception {
        AiOutputPostCheckService service = mock(AiOutputPostCheckService.class);
        when(service.check(any(AiOutputPostCheckRequest.class))).thenReturn(passingResult());
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new AiOutputPostCheckController(service)).build();
        ObjectMapper objectMapper = new ObjectMapper();
        String supplementary = new String(Character.toChars(0x1F600));
        String validJson = objectMapper.writeValueAsString(Map.of(
                "projectId", 1,
                "contentType", "TEXT",
                "content", supplementary.repeat(20_000),
                "findings", List.of(Map.of("code", supplementary.repeat(128)))));

        mockMvc.perform(post("/api/ai-output/check")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validJson))
                .andExpect(status().isOk());

        String oversizedJson = objectMapper.writeValueAsString(Map.of(
                "projectId", 1,
                "contentType", "TEXT",
                "content", supplementary.repeat(20_001)));
        mockMvc.perform(post("/api/ai-output/check")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(oversizedJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void checkForwardsReadOnlyPostCheckRequest() {
        AiOutputPostCheckService service = mock(AiOutputPostCheckService.class);
        AiOutputPostCheckRequest request = new AiOutputPostCheckRequest(
                1L,
                AiOutputContentType.MARKDOWN,
                "请使用 field:1:10。",
                null);
        AiOutputPostCheckResult result = new AiOutputPostCheckResult(
                "dataspec-ai-output-postcheck",
                1,
                1L,
                AiOutputPostCheckStatus.PASS,
                true,
                new AiOutputPostCheckSummary(1, 1, 0, 0, 0, 0, 0),
                List.of(),
                List.of(),
                List.of(),
                List.of("dataspec://refs/field:1:10"),
                List.of("可以复制或下载该 AI 产物。"));
        when(service.check(request)).thenReturn(result);
        AiOutputPostCheckController controller = new AiOutputPostCheckController(service);

        var response = controller.check(request);

        assertEquals(AiOutputPostCheckStatus.PASS, response.getData().status());
        verify(service).check(request);
    }

    private AiOutputPostCheckResult passingResult() {
        return new AiOutputPostCheckResult(
                AiOutputPostCheckResult.KIND,
                AiOutputPostCheckResult.SCHEMA_VERSION,
                1L,
                AiOutputPostCheckStatus.PASS,
                true,
                new AiOutputPostCheckSummary(0, 0, 0, 0, 0, 0, 0),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of());
    }
}
