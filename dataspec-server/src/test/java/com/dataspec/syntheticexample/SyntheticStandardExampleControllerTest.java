package com.dataspec.syntheticexample;

import com.dataspec.common.exception.BizException;
import com.dataspec.common.exception.GlobalExceptionHandler;
import com.dataspec.syntheticexample.controller.SyntheticStandardExampleController;
import com.dataspec.syntheticexample.model.SyntheticExampleDiagnostic;
import com.dataspec.syntheticexample.model.SyntheticExampleSafety;
import com.dataspec.syntheticexample.model.SyntheticExampleSourceSummary;
import com.dataspec.syntheticexample.model.SyntheticStandardExamplePackage;
import com.dataspec.syntheticexample.service.SyntheticStandardExampleService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 合成标准样例接口测试。
 */
class SyntheticStandardExampleControllerTest {

    @Test
    void generate_returnsServicePackage() {
        SyntheticStandardExampleService service = mock(SyntheticStandardExampleService.class);
        SyntheticStandardExamplePackage pkg = packageFixture(1L, "user", "sha256-demo", 4);
        when(service.generate(1L, "user", 4)).thenReturn(pkg);
        SyntheticStandardExampleController controller = new SyntheticStandardExampleController(service);

        var response = controller.generate(1L, "user", 4);

        assertEquals(200, response.getCode());
        assertEquals("sha256-demo", response.getData().specHash());
        verify(service).generate(1L, "user", 4);
    }

    @Test
    void generateHttpRouteBindsQueryParamsAndAllowsServiceDefaultMaxCases() throws Exception {
        SyntheticStandardExampleService service = mock(SyntheticStandardExampleService.class);
        when(service.generate(7L, "order", null))
                .thenReturn(packageFixture(7L, "order", "sha256-route", 6));
        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new SyntheticStandardExampleController(service))
                .build();

        mockMvc.perform(get("/api/synthetic-examples/generate")
                        .param("projectId", "7")
                        .param("scenario", "order"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.kind").value("dataspec.synthetic-standard-examples"))
                .andExpect(jsonPath("$.data.projectId").value(7))
                .andExpect(jsonPath("$.data.scenario").value("order"))
                .andExpect(jsonPath("$.data.specHash").value("sha256-route"))
                .andExpect(jsonPath("$.data.generationParams.maxCases").value(6))
                .andExpect(jsonPath("$.data.safety.readOnly").value(true))
                .andExpect(jsonPath("$.data.safety.writesProject").value(false));

        verify(service).generate(7L, "order", null);
    }

    @Test
    void generateHttpRouteReturnsSanitizedValidationErrorForUnsupportedScenario() throws Exception {
        SyntheticStandardExampleService service = mock(SyntheticStandardExampleService.class);
        when(service.generate(7L, "token=raw-secret", 4))
                .thenThrow(new BizException("不支持场景 token=raw-secret。支持: user, order, payment, audit"));
        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new SyntheticStandardExampleController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        mockMvc.perform(get("/api/synthetic-examples/generate")
                        .param("projectId", "7")
                        .param("scenario", "token=raw-secret")
                        .param("maxCases", "4"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value(containsString("[REDACTED]")))
                .andExpect(content().string(not(containsString("raw-secret"))));

        verify(service).generate(7L, "token=raw-secret", 4);
    }

    private SyntheticStandardExamplePackage packageFixture(Long projectId, String scenario, String specHash, int maxCases) {
        return new SyntheticStandardExamplePackage(
                "dataspec.synthetic-standard-examples",
                1,
                projectId,
                scenario,
                specHash,
                Map.of("scenario", scenario, "maxCases", maxCases),
                new SyntheticExampleSourceSummary(1, 0, 0, true, List.of("user_id")),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(new SyntheticExampleDiagnostic("SYNTHETIC_FALLBACK_USED", "INFO", "使用内置场景补齐")),
                List.of(),
                new SyntheticExampleSafety(true, false, false, false, List.of()),
                List.of("人工审核后再采纳为 usage example")
        );
    }
}
