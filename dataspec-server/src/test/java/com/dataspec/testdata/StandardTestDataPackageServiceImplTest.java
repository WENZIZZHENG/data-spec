package com.dataspec.testdata;

import com.dataspec.common.exception.BizException;
import com.dataspec.enumdict.entity.EnumValue;
import com.dataspec.enumdict.service.EnumDictService;
import com.dataspec.field.entity.Field;
import com.dataspec.field.service.FieldService;
import com.dataspec.security.context.DataSpecSecurityContext;
import com.dataspec.security.model.ApiTokenPrincipal;
import com.dataspec.testdata.model.StandardTestDataPackage;
import com.dataspec.testdata.model.StandardTestDataPackageReq;
import com.dataspec.testdata.service.impl.StandardTestDataPackageServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 标准测试数据包生成服务测试。
 */
class StandardTestDataPackageServiceImplTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @AfterEach
    void tearDown() {
        DataSpecSecurityContext.clear();
    }

    @Test
    void generate_buildsDeterministicCasesMockSeedCoverageAndSafety() throws Exception {
        FieldService fieldService = mock(FieldService.class);
        EnumDictService enumDictService = mock(EnumDictService.class);
        when(fieldService.listByProject(1L)).thenReturn(List.of(
                field(1L, "mobile_no", "varchar(20)", "手机号", "联系方式 password=raw-secret", "contact", "mobile", null, false),
                field(2L, "total_amount", "numeric(18,2)", "订单金额", "订单支付金额", "money", "money", null, false),
                field(3L, "order_status", "varchar(20)", "订单状态", "订单状态枚举", "status", "enum", 10L, false)
        ));
        when(enumDictService.listValues(10L)).thenReturn(List.of(
                enumValue(100L, "PAID", "已支付", "enabled"),
                enumValue(101L, "CANCELLED", "已取消", "deprecated")
        ));
        StandardTestDataPackageServiceImpl service = new StandardTestDataPackageServiceImpl(fieldService, enumDictService);
        StandardTestDataPackageReq req = new StandardTestDataPackageReq(
                1L,
                List.of("mobile_no", "total_amount", "order_status"),
                "order",
                10,
                3,
                2,
                "postgres");

        StandardTestDataPackage first = service.generate(req);
        StandardTestDataPackage second = service.generate(req);

        assertEquals("dataspec.standard-test-data-package", first.kind());
        assertEquals(1, first.schemaVersion());
        assertEquals(1L, first.projectId());
        assertEquals(first.specHash(), second.specHash());
        assertEquals(3, first.sourceSummary().selectedFieldCount());
        assertFalse(first.testDataCases().isEmpty());
        assertTrue(first.testDataCases().stream().anyMatch(item ->
                "mobile_no".equals(item.fieldName()) && "VALID".equals(item.caseType())));
        assertTrue(first.testDataCases().stream().anyMatch(item ->
                "mobile_no".equals(item.fieldName()) && "INVALID".equals(item.caseType())));
        assertTrue(first.testDataCases().stream().anyMatch(item ->
                "total_amount".equals(item.fieldName()) && "BOUNDARY".equals(item.caseType())));
        assertTrue(first.testDataCases().stream().anyMatch(item ->
                "order_status".equals(item.fieldName()) && "PAID".equals(item.value())));
        assertFalse(first.seedProfiles().isEmpty());
        assertTrue(first.seedProfiles().stream().anyMatch(item -> "JSON".equals(item.format())));
        assertTrue(first.seedProfiles().stream().anyMatch(item -> "CSV".equals(item.format())));
        assertTrue(first.seedProfiles().stream().anyMatch(item -> "SQL".equals(item.format())));
        assertFalse(first.mockPayloads().isEmpty());
        assertTrue(first.coverageReport().coveredFieldCount() >= 3);
        assertTrue(first.safety().readOnly());
        assertFalse(first.safety().writesProject());
        assertFalse(first.safety().writesBusinessRepo());
        assertFalse(first.safety().containsRealBusinessRows());
        assertFalse(first.safety().externalLlmUsed());

        String json = objectMapper.writeValueAsString(first);
        assertFalse(json.contains("raw-secret"));
        assertTrue(json.contains("[REDACTED]"));
    }

    @Test
    void generate_changesSpecHashWhenFieldFormatChanges() {
        FieldService fieldService = mock(FieldService.class);
        EnumDictService enumDictService = mock(EnumDictService.class);
        Field amountInYuan = field(1L, "total_amount", "numeric(18,2)", "金额", "订单金额", "money", "money", null, false);
        amountInYuan.setFormatUnit("yuan");
        Field amountInCent = field(1L, "total_amount", "numeric(18,2)", "金额", "订单金额", "money", "money", null, false);
        amountInCent.setFormatUnit("cent");
        when(fieldService.listByProject(1L))
                .thenReturn(List.of(amountInYuan))
                .thenReturn(List.of(amountInCent));
        StandardTestDataPackageServiceImpl service = new StandardTestDataPackageServiceImpl(fieldService, enumDictService);
        StandardTestDataPackageReq req = new StandardTestDataPackageReq(1L, List.of("total_amount"), null, 5, 3, 1, null);

        StandardTestDataPackage first = service.generate(req);
        StandardTestDataPackage second = service.generate(req);

        assertNotEquals(first.specHash(), second.specHash());
    }

    @Test
    void generate_usesFallbackAndMarksMissingConstraintsWhenMetadataIsSparse() {
        FieldService fieldService = mock(FieldService.class);
        EnumDictService enumDictService = mock(EnumDictService.class);
        when(fieldService.listByProject(1L)).thenReturn(List.of());
        StandardTestDataPackageServiceImpl service = new StandardTestDataPackageServiceImpl(fieldService, enumDictService);

        StandardTestDataPackage result = service.generate(new StandardTestDataPackageReq(1L, List.of(), "audit", 4, 2, 1, null));

        assertTrue(result.sourceSummary().fallbackUsed());
        assertFalse(result.testDataCases().isEmpty());
        assertFalse(result.coverageReport().missingConstraints().isEmpty());
        assertTrue(result.diagnostics().stream().anyMatch(item -> "TEST_DATA_FALLBACK_USED".equals(item.code())));
    }

    @Test
    void generate_reportsBoundTruncationDiagnosticsForDirectApiCall() {
        FieldService fieldService = mock(FieldService.class);
        EnumDictService enumDictService = mock(EnumDictService.class);
        when(fieldService.listByProject(1L)).thenReturn(List.of(
                field(1L, "mobile_no", "varchar(20)", "手机号", "手机号", "contact", "mobile", null, false)
        ));
        StandardTestDataPackageServiceImpl service = new StandardTestDataPackageServiceImpl(fieldService, enumDictService);

        StandardTestDataPackage result = service.generate(new StandardTestDataPackageReq(
                1L,
                List.of("mobile_no"),
                null,
                999,
                99,
                999,
                null));

        assertEquals(50, result.generationParams().get("maxFields"));
        assertEquals(3, result.generationParams().get("casesPerField"));
        assertEquals(20, result.generationParams().get("seedRowCount"));
        assertTrue(result.diagnostics().stream().anyMatch(item ->
                "TEST_DATA_BOUND_TRUNCATED".equals(item.code()) && item.message().contains("maxFields")));
    }

    @Test
    void generate_rejectsUnknownFieldSelectorBeforeFallback() {
        FieldService fieldService = mock(FieldService.class);
        EnumDictService enumDictService = mock(EnumDictService.class);
        when(fieldService.listByProject(1L)).thenReturn(List.of(
                field(1L, "mobile_no", "varchar(20)", "手机号", "手机号", "contact", "mobile", null, false)
        ));
        StandardTestDataPackageServiceImpl service = new StandardTestDataPackageServiceImpl(fieldService, enumDictService);

        BizException ex = assertThrows(BizException.class, () -> service.generate(new StandardTestDataPackageReq(
                1L,
                List.of("missing_field"),
                null,
                5,
                3,
                1,
                null)));

        assertTrue(ex.getMessage().contains("fieldNames 不存在于项目标准"));
    }

    @Test
    void generate_rejectsProjectOutsideTokenScopeBeforeReadingFields() {
        FieldService fieldService = mock(FieldService.class);
        EnumDictService enumDictService = mock(EnumDictService.class);
        DataSpecSecurityContext.set(new ApiTokenPrincipal("limited", "tester", false, Set.of(2L)));
        StandardTestDataPackageServiceImpl service = new StandardTestDataPackageServiceImpl(fieldService, enumDictService);

        BizException ex = assertThrows(BizException.class, () -> service.generate(new StandardTestDataPackageReq(
                1L,
                List.of("mobile_no"),
                null,
                5,
                3,
                1,
                null)));

        assertEquals(403, ex.getCode());
        assertTrue(ex.getMessage().contains("无权访问项目"));
        verify(fieldService, never()).listByProject(1L);
    }

    @Test
    void generate_rejectsUnsafeOrOversizedSelectorsWithoutEchoingSecret() {
        StandardTestDataPackageServiceImpl service = new StandardTestDataPackageServiceImpl(
                mock(FieldService.class),
                mock(EnumDictService.class));
        StandardTestDataPackageReq req = new StandardTestDataPackageReq(
                1L,
                List.of("token=raw-secret"),
                null,
                5,
                3,
                1,
                null);

        BizException ex = assertThrows(BizException.class, () -> service.generate(req));

        assertFalse(ex.getMessage().contains("raw-secret"));
        assertTrue(ex.getMessage().contains("[REDACTED]"));
    }

    private Field field(
            Long id,
            String name,
            String dataType,
            String displayName,
            String comment,
            String category,
            String formatType,
            Long codeSetId,
            boolean sensitive) {
        Field field = new Field();
        field.setId(id);
        field.setProjectId(1L);
        field.setName(name);
        field.setDataType(dataType);
        field.setDisplayName(displayName);
        field.setComment(comment);
        field.setCategory(category);
        field.setFormatType(formatType);
        field.setCodeSetId(codeSetId);
        field.setSensitive(sensitive);
        field.setStatus("enabled");
        field.setValidExamplesJson("[\"13800138000\",\"PAID\",\"88.88\"]");
        field.setInvalidExamplesJson("[\"not-valid\",\"UNKNOWN\",\"-1\"]");
        return field;
    }

    private EnumValue enumValue(Long id, String value, String label, String status) {
        EnumValue enumValue = new EnumValue();
        enumValue.setId(id);
        enumValue.setEnumId(10L);
        enumValue.setValue(value);
        enumValue.setLabel(label);
        enumValue.setStatus(status);
        return enumValue;
    }
}
