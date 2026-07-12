package com.dataspec.testdata;

import com.dataspec.common.exception.BizException;
import com.dataspec.common.exception.GlobalExceptionHandler;
import com.dataspec.testdata.controller.StandardTestDataPackageController;
import com.dataspec.testdata.model.StandardTestDataPackage;
import com.dataspec.testdata.model.StandardTestDataPackageReq;
import com.dataspec.testdata.model.TestDataCoverageReport;
import com.dataspec.testdata.model.TestDataSafety;
import com.dataspec.testdata.model.TestDataSourceSummary;
import com.dataspec.testdata.service.StandardTestDataPackageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 标准测试数据包接口测试。
 */
class StandardTestDataPackageControllerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void generate_returnsServicePackage() {
        StandardTestDataPackageService service = mock(StandardTestDataPackageService.class);
        StandardTestDataPackage pkg = packageFixture(7L);
        StandardTestDataPackageReq req = new StandardTestDataPackageReq(7L, List.of("mobile_no"), "user", 5, 3, 1, "postgres");
        when(service.generate(req)).thenReturn(pkg);
        StandardTestDataPackageController controller = new StandardTestDataPackageController(service);

        var response = controller.generate(req);

        assertEquals(200, response.getCode());
        assertEquals("test-data-hash", response.getData().specHash());
        verify(service).generate(req);
    }

    @Test
    void generateHttpRouteBindsJsonBody() throws Exception {
        StandardTestDataPackageService service = mock(StandardTestDataPackageService.class);
        when(service.generate(argThat(req -> req != null && Long.valueOf(7L).equals(req.projectId()))))
                .thenReturn(packageFixture(7L));
        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new StandardTestDataPackageController(service))
                .build();

        mockMvc.perform(post("/api/test-data/package/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "projectId", 7,
                                "fieldNames", List.of("mobile_no"),
                                "objectScenario", "user",
                                "maxFields", 5,
                                "casesPerField", 3,
                                "seedRowCount", 1,
                                "dialect", "postgres"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.kind").value("dataspec.standard-test-data-package"))
                .andExpect(jsonPath("$.data.projectId").value(7))
                .andExpect(jsonPath("$.data.specHash").value("test-data-hash"))
                .andExpect(jsonPath("$.data.safety.readOnly").value(true))
                .andExpect(jsonPath("$.data.safety.writesProject").value(false));
    }

    @Test
    void generateHttpRouteReturnsSanitizedError() throws Exception {
        StandardTestDataPackageService service = mock(StandardTestDataPackageService.class);
        when(service.generate(argThat(req -> req != null && req.fieldNames().contains("token=raw-secret"))))
                .thenThrow(new BizException("fieldNames 包含敏感片段: token=[REDACTED]"));
        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new StandardTestDataPackageController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        mockMvc.perform(post("/api/test-data/package/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "projectId", 7,
                                "fieldNames", List.of("token=raw-secret")))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value(containsString("[REDACTED]")))
                .andExpect(content().string(not(containsString("raw-secret"))));
    }

    private StandardTestDataPackage packageFixture(Long projectId) {
        return new StandardTestDataPackage(
                "dataspec.standard-test-data-package",
                1,
                projectId,
                "test-data-hash",
                Map.of("objectScenario", "user"),
                new TestDataSourceSummary(1, 1, 0, false, List.of("mobile_no"), List.of("project-field")),
                List.of(),
                List.of(),
                List.of(),
                new TestDataCoverageReport(1, 1, 3, "FIELD_ONLY", List.of(), List.of()),
                List.of(),
                new TestDataSafety(true, false, false, false, false, false, List.of()),
                List.of("人工审核后再使用")
        );
    }
}
