package com.dataspec.syntheticexample;

import com.dataspec.field.service.FieldService;
import com.dataspec.syntheticexample.service.impl.SyntheticStandardExampleServiceImpl;
import com.dataspec.template.service.TemplateService;
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
 * 合成标准样例 contract fixture 测试，防止 AI 消费字段漂移。
 */
class SyntheticStandardExampleContractFixtureTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void generatedPackage_keepsStableContractFieldsFromFixture() throws Exception {
        JsonNode fixture = objectMapper.readTree(readResource("fixtures/syntheticexample/contract-fields.json"));
        FieldService fieldService = mock(FieldService.class);
        TemplateService templateService = mock(TemplateService.class);
        when(fieldService.listByProject(1L)).thenReturn(List.of());
        when(templateService.listByProject(1L)).thenReturn(List.of());
        SyntheticStandardExampleServiceImpl service = new SyntheticStandardExampleServiceImpl(fieldService, templateService);

        for (JsonNode scenarioNode : fixture.path("supportedScenarios")) {
            JsonNode generated = objectMapper.valueToTree(service.generate(1L, scenarioNode.asText(), 4));

            assertThat(fieldNames(fixture.path("requiredTopLevelFields")))
                    .allSatisfy(field -> assertThat(generated.has(field)).as(field).isTrue());
            assertThat(fieldNames(fixture.path("requiredSafetyFields")))
                    .allSatisfy(field -> assertThat(generated.path("safety").has(field)).as(field).isTrue());
            assertThat(generated.path("safety").path("readOnly").asBoolean()).isTrue();
            assertThat(generated.path("safety").path("writesProject").asBoolean()).isFalse();
            assertThat(generated.path("goodSql").isEmpty()).isFalse();
            assertThat(generated.path("badSql").get(0).path("expectedDiagnosticIds").isEmpty()).isFalse();
        }
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
