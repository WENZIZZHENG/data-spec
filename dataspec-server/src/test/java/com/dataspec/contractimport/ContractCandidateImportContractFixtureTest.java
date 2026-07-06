package com.dataspec.contractimport;

import com.dataspec.contractimport.model.ContractCandidatePreviewPackage;
import com.dataspec.contractimport.model.ContractCandidatePreviewReq;
import com.dataspec.contractimport.service.impl.ContractCandidateImportServiceImpl;
import com.dataspec.field.service.FieldService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 契约候选导入后端 golden fixture 测试，防止稳定输出字段和脱敏规则漂移。
 */
class ContractCandidateImportContractFixtureTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void generatedPreview_keepsStableContractFieldsFromFixture() throws Exception {
        JsonNode fixture = objectMapper.readTree(readResource("fixtures/contractimport/contract-preview-fields.json"));
        FieldService fieldService = mock(FieldService.class);
        when(fieldService.listByProject(1L)).thenReturn(List.of());
        ContractCandidateImportServiceImpl service = new ContractCandidateImportServiceImpl(fieldService);

        ContractCandidatePreviewPackage generated = service.preview(new ContractCandidatePreviewReq(
                1L,
                "openapi",
                "contracts/order.yaml?token=fixture-secret",
                """
                openapi: 3.0.3
                components:
                  schemas:
                    Order:
                      type: object
                      required: [orderId]
                      properties:
                        orderId:
                          type: integer
                          format: int64
                          description: "订单ID Authorization: Bearer fixture-secret"
                          example: 1001
                """,
                100));
        JsonNode generatedJson = objectMapper.valueToTree(generated);

        assertThat(fieldNames(fixture.path("requiredTopLevelFields")))
                .allSatisfy(field -> assertThat(generatedJson.has(field)).as(field).isTrue());
        assertThat(fieldNames(fixture.path("requiredCandidateFields")))
                .allSatisfy(field -> assertThat(generatedJson.path("candidateFields").get(0).has(field)).as(field).isTrue());
        assertThat(fieldNames(fixture.path("requiredSafetyFields")))
                .allSatisfy(field -> assertThat(generatedJson.path("safety").has(field)).as(field).isTrue());
        assertThat(generatedJson.path("safety").path("readOnly").asBoolean()).isTrue();
        assertThat(generatedJson.path("safety").path("writesProject").asBoolean()).isFalse();
        assertThat(generatedJson.path("safety").path("externalNetworkUsed").asBoolean()).isFalse();
        assertThat(generatedJson.path("safety").path("externalLlmUsed").asBoolean()).isFalse();

        String json = objectMapper.writeValueAsString(generated);
        assertThat(json).doesNotContain("fixture-secret");
        assertThat(json).contains("[REDACTED]");
    }

    private List<String> fieldNames(JsonNode fields) {
        List<String> result = new ArrayList<>();
        fields.forEach(field -> result.add(field.asText()));
        return result;
    }

    private String readResource(String path) {
        try (var input = getClass().getClassLoader().getResourceAsStream(path)) {
            assertNotNull(input, "测试资源不存在: " + path);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("读取测试资源失败: " + path, e);
        }
    }
}
