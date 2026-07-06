package com.dataspec.contractimport;

import com.dataspec.common.exception.BizException;
import com.dataspec.common.exception.GlobalExceptionHandler;
import com.dataspec.contractimport.controller.ContractCandidateImportController;
import com.dataspec.contractimport.model.ContractCandidateDiagnostic;
import com.dataspec.contractimport.model.ContractCandidateField;
import com.dataspec.contractimport.model.ContractCandidatePreviewPackage;
import com.dataspec.contractimport.model.ContractCandidatePreviewReq;
import com.dataspec.contractimport.model.ContractCandidateSafety;
import com.dataspec.contractimport.model.ContractCandidateSummary;
import com.dataspec.contractimport.service.ContractCandidateImportService;
import com.dataspec.standardcandidate.model.StandardCandidateCreateReq;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 契约候选导入 controller 测试，覆盖路由绑定、校验和错误脱敏。
 */
class ContractCandidateImportControllerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void preview_returnsServicePackage() {
        ContractCandidateImportService service = mock(ContractCandidateImportService.class);
        ContractCandidatePreviewReq req = requestFixture();
        ContractCandidatePreviewPackage pkg = packageFixture(1L, "openapi", "sha256-demo");
        when(service.preview(req)).thenReturn(pkg);
        ContractCandidateImportController controller = new ContractCandidateImportController(service);

        var response = controller.preview(req);

        assertEquals(200, response.getCode());
        assertEquals("sha256-demo", response.getData().contractHash());
        verify(service).preview(req);
    }

    @Test
    void previewHttpRouteBindsJsonBodyAndReturnsSafetyMetadata() throws Exception {
        ContractCandidateImportService service = mock(ContractCandidateImportService.class);
        when(service.preview(any())).thenReturn(packageFixture(7L, "json-schema", "sha256-route"));
        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new ContractCandidateImportController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        mockMvc.perform(post("/api/contract-import/preview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ContractCandidatePreviewReq(
                                7L,
                                "json-schema",
                                "contracts/customer.schema.json",
                                "{\"type\":\"object\",\"properties\":{\"customerId\":{\"type\":\"string\"}}}",
                                null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.kind").value("dataspec.contract-candidate-preview"))
                .andExpect(jsonPath("$.data.projectId").value(7))
                .andExpect(jsonPath("$.data.sourceKind").value("json-schema"))
                .andExpect(jsonPath("$.data.contractHash").value("sha256-route"))
                .andExpect(jsonPath("$.data.candidateFields[0].candidateName").value("customer_id"))
                .andExpect(jsonPath("$.data.candidateFields[0].inboxPayload.sourceType").value("CONTRACT_IMPORT"))
                .andExpect(jsonPath("$.data.safety.readOnly").value(true))
                .andExpect(jsonPath("$.data.safety.writesProject").value(false))
                .andExpect(jsonPath("$.data.safety.externalNetworkUsed").value(false))
                .andExpect(jsonPath("$.data.safety.externalLlmUsed").value(false));
    }

    @Test
    void previewHttpRouteRejectsMissingBodyFieldsBeforeServiceCall() throws Exception {
        ContractCandidateImportService service = mock(ContractCandidateImportService.class);
        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new ContractCandidateImportController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        mockMvc.perform(post("/api/contract-import/preview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "projectId": 7,
                                  "sourceKind": "openapi",
                                  "sourcePath": "contracts/order.yaml"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value(containsString("contractContent")));
    }

    @Test
    void previewHttpRouteRejectsMaxCandidatesOverLimit() throws Exception {
        ContractCandidateImportService service = mock(ContractCandidateImportService.class);
        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new ContractCandidateImportController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        mockMvc.perform(post("/api/contract-import/preview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ContractCandidatePreviewReq(
                                7L,
                                "openapi",
                                "contracts/order.yaml",
                                "openapi: 3.0.3",
                                999))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value(containsString("maxCandidates")));
    }

    @Test
    void previewHttpRouteReturnsSanitizedServiceError() throws Exception {
        ContractCandidateImportService service = mock(ContractCandidateImportService.class);
        when(service.preview(any()))
                .thenThrow(new BizException("不支持的契约来源类型: token=raw-secret。支持: openapi, json-schema, protobuf"));
        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new ContractCandidateImportController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        mockMvc.perform(post("/api/contract-import/preview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ContractCandidatePreviewReq(
                                7L,
                                "token=raw-secret",
                                "contracts/order.yaml",
                                "{}",
                                100))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value(containsString("[REDACTED]")))
                .andExpect(content().string(not(containsString("raw-secret"))));
    }

    private ContractCandidatePreviewReq requestFixture() {
        return new ContractCandidatePreviewReq(
                1L,
                "openapi",
                "contracts/order.yaml",
                "openapi: 3.0.3",
                100);
    }

    private ContractCandidatePreviewPackage packageFixture(Long projectId, String sourceKind, String hash) {
        return new ContractCandidatePreviewPackage(
                "dataspec.contract-candidate-preview",
                1,
                projectId,
                sourceKind,
                "contracts/customer.schema.json",
                hash,
                new ContractCandidateSummary(1, 1, 0, 0, 0, false),
                List.of(new ContractCandidateField(
                        sourceKind + ":customer_id",
                        "customer_id",
                        "客户ID",
                        "varchar(255)",
                        true,
                        List.of(),
                        List.of(),
                        "#/properties/customerId",
                        1,
                        82,
                        List.of(),
                        "CREATE_CANDIDATE",
                        new StandardCandidateCreateReq(
                                projectId,
                                "customer_id",
                                "客户ID",
                                "varchar(255)",
                                "客户ID",
                                "CONTRACT_IMPORT",
                                sourceKind + ":#/properties/customerId",
                                "{}",
                                82))),
                List.of(new ContractCandidateDiagnostic("NOOP", "INFO", "测试诊断", "#")),
                new ContractCandidateSafety(true, false, false, false, false, List.of()),
                List.of("人工审核后再采纳"))
        ;
    }
}
