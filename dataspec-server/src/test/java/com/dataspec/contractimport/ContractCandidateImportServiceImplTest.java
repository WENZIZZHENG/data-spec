package com.dataspec.contractimport;

import com.dataspec.common.exception.BizException;
import com.dataspec.contractimport.model.ContractCandidateField;
import com.dataspec.contractimport.model.ContractCandidatePreviewPackage;
import com.dataspec.contractimport.model.ContractCandidatePreviewReq;
import com.dataspec.contractimport.service.impl.ContractCandidateImportServiceImpl;
import com.dataspec.field.entity.Field;
import com.dataspec.field.service.FieldService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 契约候选导入预览服务测试，先固定 API/CLI 共用的只读预览契约。
 */
class ContractCandidateImportServiceImplTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void preview_extractsOpenApiPropertiesAndMatchesExistingStandardsSafely() throws Exception {
        FieldService fieldService = mock(FieldService.class);
        when(fieldService.listByProject(1L)).thenReturn(List.of(
                field(11L, "order_status", "varchar(32)", "订单状态", "订单生命周期状态")
        ));
        ContractCandidateImportServiceImpl service = new ContractCandidateImportServiceImpl(fieldService);

        ContractCandidatePreviewPackage result = service.preview(new ContractCandidatePreviewReq(
                1L,
                "openapi",
                "contracts/order-openapi.yaml?token=raw-token",
                """
                openapi: 3.0.3
                components:
                  schemas:
                    Order:
                      type: object
                      required: [orderId, orderStatus]
                      properties:
                        orderId:
                          type: integer
                          format: int64
                          description: 订单主键
                          example: 1001
                        orderStatus:
                          type: string
                          description: "订单状态 password=raw-secret"
                          enum: [CREATED, PAID]
                          example: PAID
                """,
                100));

        assertThat(result.kind()).isEqualTo("dataspec.contract-candidate-preview");
        assertThat(result.schemaVersion()).isEqualTo(1);
        assertThat(result.projectId()).isEqualTo(1L);
        assertThat(result.sourceKind()).isEqualTo("openapi");
        assertThat(result.sourcePath()).contains("[REDACTED]");
        assertThat(result.contractHash()).hasSize(64);
        assertThat(result.summary().candidateCount()).isEqualTo(2);
        assertThat(result.safety().readOnly()).isTrue();
        assertThat(result.safety().writesProject()).isFalse();
        assertThat(result.safety().externalNetworkUsed()).isFalse();
        assertThat(result.safety().externalLlmUsed()).isFalse();
        assertThat(result.safety().containsRealBusinessRows()).isFalse();

        ContractCandidateField orderId = candidate(result, "order_id");
        assertThat(orderId.displayName()).isEqualTo("订单主键");
        assertThat(orderId.dataType()).isEqualTo("bigint");
        assertThat(orderId.required()).isTrue();
        assertThat(orderId.exampleValues()).containsExactly("1001");
        assertThat(orderId.sourcePath()).isEqualTo("#/components/schemas/Order/properties/orderId");
        assertThat(orderId.recommendedAction()).isEqualTo("CREATE_CANDIDATE");
        assertThat(orderId.inboxPayload().sourceType()).isEqualTo("CONTRACT_IMPORT");

        ContractCandidateField status = candidate(result, "order_status");
        assertThat(status.enumValues()).containsExactly("CREATED", "PAID");
        assertThat(status.recommendedAction()).isEqualTo("MERGE_EXISTING");
        assertThat(status.conflictReasons()).anySatisfy(reason ->
                assertThat(reason).contains("已有标准字段"));

        String json = objectMapper.writeValueAsString(result);
        assertThat(json).doesNotContain("raw-token", "raw-secret");
        assertThat(json).contains("[REDACTED]");
    }

    @Test
    void preview_extractsJsonSchemaRequiredFieldsAndUsesDeterministicHash() {
        FieldService fieldService = mock(FieldService.class);
        when(fieldService.listByProject(2L)).thenReturn(List.of());
        ContractCandidateImportServiceImpl service = new ContractCandidateImportServiceImpl(fieldService);
        ContractCandidatePreviewReq req = new ContractCandidatePreviewReq(
                2L,
                "json-schema",
                "contracts/customer.schema.json",
                """
                {
                  "$schema": "https://json-schema.org/draft/2020-12/schema",
                  "type": "object",
                  "required": ["customerId"],
                  "properties": {
                    "customerId": {
                      "type": "string",
                      "description": "客户ID"
                    },
                    "creditAmount": {
                      "type": "number",
                      "description": "授信金额",
                      "example": "100.25"
                    }
                  }
                }
                """,
                100);

        ContractCandidatePreviewPackage first = service.preview(req);
        ContractCandidatePreviewPackage second = service.preview(req);
        ContractCandidatePreviewPackage changed = service.preview(new ContractCandidatePreviewReq(
                2L,
                "json-schema",
                "contracts/customer.schema.json",
                req.contractContent().replace("授信金额", "授信金额-变更"),
                100));

        assertThat(first.contractHash()).isEqualTo(second.contractHash());
        assertThat(first.contractHash()).isNotEqualTo(changed.contractHash());
        assertThat(candidate(first, "customer_id").required()).isTrue();
        assertThat(candidate(first, "customer_id").sourcePath()).isEqualTo("#/properties/customerId");
        assertThat(candidate(first, "credit_amount").dataType()).isEqualTo("decimal");
    }

    @Test
    void preview_redactsSensitivePropertyNamesFromSourcePathsAndInboxPayload() throws Exception {
        FieldService fieldService = mock(FieldService.class);
        when(fieldService.listByProject(20L)).thenReturn(List.of());
        ContractCandidateImportServiceImpl service = new ContractCandidateImportServiceImpl(fieldService);

        ContractCandidatePreviewPackage result = service.preview(new ContractCandidatePreviewReq(
                20L,
                "json-schema",
                "contracts/credential.schema.json",
                """
                {
                  "type": "object",
                  "properties": {
                    "api_key=raw-secret": {
                      "type": "string",
                      "description": "第三方接口凭据字段"
                    }
                  }
                }
                """,
                100));

        ContractCandidateField field = result.candidateFields().getFirst();
        assertThat(field.sourcePath()).contains("[REDACTED]");
        assertThat(field.inboxPayload().sourceRef()).contains("[REDACTED]");
        assertThat(field.inboxPayload().evidenceJson()).contains("[REDACTED]");

        String json = objectMapper.writeValueAsString(result);
        assertThat(json).doesNotContain("raw-secret");
    }

    @Test
    void preview_hashChangesWhenLongContractContentChangesAfterOutputSummaryLimit() {
        FieldService fieldService = mock(FieldService.class);
        when(fieldService.listByProject(21L)).thenReturn(List.of());
        ContractCandidateImportServiceImpl service = new ContractCandidateImportServiceImpl(fieldService);
        String sharedPrefix = "A".repeat(560);

        ContractCandidatePreviewPackage first = service.preview(new ContractCandidatePreviewReq(
                21L,
                "json-schema",
                "contracts/long.schema.json",
                jsonSchemaWithLongDescription(sharedPrefix + "FIRST"),
                100));
        ContractCandidatePreviewPackage changedAfter500Chars = service.preview(new ContractCandidatePreviewReq(
                21L,
                "json-schema",
                "contracts/long.schema.json",
                jsonSchemaWithLongDescription(sharedPrefix + "SECOND"),
                100));

        assertThat(first.contractHash()).isNotEqualTo(changedAfter500Chars.contractHash());
    }

    @Test
    void preview_extractsProtobufTextAndDescriptorJsonFields() {
        FieldService fieldService = mock(FieldService.class);
        when(fieldService.listByProject(3L)).thenReturn(List.of());
        ContractCandidateImportServiceImpl service = new ContractCandidateImportServiceImpl(fieldService);

        ContractCandidatePreviewPackage protoText = service.preview(new ContractCandidatePreviewReq(
                3L,
                "protobuf",
                "contracts/order.proto",
                """
                message OrderCreated {
                  // 订单ID
                  string order_id = 1;
                  int64 paid_amount = 2;
                  bool test_flag = 3;
                }
                """,
                100));

        assertThat(candidate(protoText, "order_id").displayName()).isEqualTo("订单ID");
        assertThat(candidate(protoText, "order_id").sourcePath()).isEqualTo("proto://OrderCreated/order_id#1");
        assertThat(candidate(protoText, "paid_amount").dataType()).isEqualTo("bigint");
        assertThat(candidate(protoText, "test_flag").dataType()).isEqualTo("boolean");

        ContractCandidatePreviewPackage descriptor = service.preview(new ContractCandidatePreviewReq(
                3L,
                "protobuf",
                "contracts/payment-descriptor.json",
                """
                {
                  "messageType": [
                    {
                      "name": "Payment",
                      "field": [
                        {"name": "payment_id", "number": 1, "type": "TYPE_STRING", "jsonName": "paymentId"},
                        {"name": "paid_at", "number": 2, "type": "TYPE_INT64", "jsonName": "paidAt"}
                      ]
                    }
                  ]
                }
                """,
                100));

        assertThat(candidate(descriptor, "payment_id").sourcePath()).isEqualTo("descriptor://Payment/payment_id#1");
        assertThat(candidate(descriptor, "paid_at").dataType()).isEqualTo("bigint");
    }

    @Test
    void preview_extractsOpenApiArrayResponseItemProperties() {
        FieldService fieldService = mock(FieldService.class);
        when(fieldService.listByProject(22L)).thenReturn(List.of());
        ContractCandidateImportServiceImpl service = new ContractCandidateImportServiceImpl(fieldService);

        ContractCandidatePreviewPackage result = service.preview(new ContractCandidatePreviewReq(
                22L,
                "openapi",
                "contracts/order-list.yaml",
                """
                openapi: 3.0.3
                paths:
                  /orders:
                    get:
                      responses:
                        "200":
                          description: ok
                          content:
                            application/json:
                              schema:
                                type: array
                                items:
                                  type: object
                                  required: [arrayOrderId]
                                  properties:
                                    arrayOrderId:
                                      type: string
                                      description: 列表订单ID
                """,
                100));

        ContractCandidateField field = candidate(result, "array_order_id");
        assertThat(field.displayName()).isEqualTo("列表订单ID");
        assertThat(field.required()).isTrue();
        assertThat(field.sourcePath()).contains("/items/properties/arrayOrderId");
    }

    @Test
    void preview_marksComplexSchemaReviewRequiredAndRedactsDiagnostics() throws Exception {
        FieldService fieldService = mock(FieldService.class);
        when(fieldService.listByProject(4L)).thenReturn(List.of());
        ContractCandidateImportServiceImpl service = new ContractCandidateImportServiceImpl(fieldService);

        ContractCandidatePreviewPackage result = service.preview(new ContractCandidatePreviewReq(
                4L,
                "json-schema",
                "contracts/payment.schema.json",
                """
                {
                  "type": "object",
                  "properties": {
                    "paymentMethod": {
                      "oneOf": [
                        {"type": "string"},
                        {"$ref": "#/$defs/PaymentMethod"}
                      ],
                      "description": "支付方式 api_key=raw-secret"
                    }
                  }
                }
                """,
                100));

        ContractCandidateField field = candidate(result, "payment_method");
        assertThat(field.recommendedAction()).isEqualTo("REVIEW_REQUIRED");
        assertThat(field.conflictReasons()).anySatisfy(reason ->
                assertThat(reason).contains("复杂 schema"));
        assertThat(result.diagnostics()).anySatisfy(diagnostic -> {
            assertThat(diagnostic.code()).isEqualTo("UNSUPPORTED_SCHEMA_COMPOSITION");
            assertThat(diagnostic.message()).contains("[REDACTED]");
        });

        String json = objectMapper.writeValueAsString(result);
        assertThat(json).doesNotContain("raw-secret");
        assertThat(json).contains("[REDACTED]");
    }

    @Test
    void preview_rejectsUnsupportedSourceKindWithSafeMessage() {
        ContractCandidateImportServiceImpl service = new ContractCandidateImportServiceImpl(mock(FieldService.class));

        BizException ex = assertThrows(BizException.class, () -> service.preview(new ContractCandidatePreviewReq(
                1L,
                "swagger-token=raw-secret",
                "contracts/order.yaml",
                "{}",
                100)));

        assertThat(ex.getMessage()).contains("openapi", "json-schema", "protobuf");
        assertThat(ex.getMessage()).doesNotContain("raw-secret");
        assertThat(ex.getMessage()).contains("[REDACTED]");
    }

    private ContractCandidateField candidate(ContractCandidatePreviewPackage result, String candidateName) {
        return result.candidateFields().stream()
                .filter(item -> candidateName.equals(item.candidateName()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("未找到候选字段: " + candidateName));
    }

    private String jsonSchemaWithLongDescription(String description) {
        return """
                {
                  "type": "object",
                  "properties": {
                    "stableField": {
                      "type": "string",
                      "description": "%s"
                    }
                  }
                }
                """.formatted(description);
    }

    private Field field(Long id, String name, String dataType, String displayName, String comment) {
        Field field = new Field();
        field.setId(id);
        field.setProjectId(1L);
        field.setName(name);
        field.setDataType(dataType);
        field.setDisplayName(displayName);
        field.setComment(comment);
        field.setStatus("enabled");
        return field;
    }
}
