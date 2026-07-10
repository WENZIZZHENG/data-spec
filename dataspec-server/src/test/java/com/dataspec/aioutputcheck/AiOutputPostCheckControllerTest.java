package com.dataspec.aioutputcheck;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.dataspec.aioutputcheck.controller.AiOutputPostCheckController;
import com.dataspec.aioutputcheck.model.AiOutputContentType;
import com.dataspec.aioutputcheck.model.AiOutputPostCheckRequest;
import com.dataspec.aioutputcheck.model.AiOutputPostCheckResult;
import com.dataspec.aioutputcheck.model.AiOutputPostCheckStatus;
import com.dataspec.aioutputcheck.model.AiOutputPostCheckSummary;
import com.dataspec.aioutputcheck.service.AiOutputPostCheckService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
}
