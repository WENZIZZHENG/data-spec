package com.dataspec.field;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.dataspec.businessglossary.model.GlossaryMatch;
import com.dataspec.businessglossary.service.BusinessGlossaryService;
import com.dataspec.changelog.entity.StandardChangeLog;
import com.dataspec.changelog.service.StandardChangeLogService;
import com.dataspec.common.exception.BizException;
import com.dataspec.field.entity.Field;
import com.dataspec.field.model.FieldBulkUpdatePreview;
import com.dataspec.field.model.FieldBulkUpdateReq;
import com.dataspec.field.model.FieldBulkUpdateResult;
import com.dataspec.field.model.FieldChangeUndoResult;
import com.dataspec.field.model.FieldGroupSummary;
import com.dataspec.field.model.FieldGroupingBatchUpdateReq;
import com.dataspec.field.model.FieldGroupingBatchUpdateResult;
import com.dataspec.field.model.FieldSearchReq;
import com.dataspec.field.model.FieldSearchItem;
import com.dataspec.field.model.FieldSearchResult;
import com.dataspec.field.model.FieldSuggestion;
import com.dataspec.field.repository.FieldRepository;
import com.dataspec.field.service.impl.FieldServiceImpl;
import com.dataspec.fieldhistory.model.FieldHistoricalAlias;
import com.dataspec.fieldhistory.service.FieldHistoricalAliasService;
import com.dataspec.fieldsemantic.service.FieldSemanticRuleService;
import com.dataspec.metric.service.MetricDefinitionService;
import com.dataspec.querynormalization.model.QueryTokenResolutionStatus;
import com.dataspec.querynormalization.service.impl.QueryNormalizationServiceImpl;
import com.dataspec.querynormalization.tokenizer.NameLexicalTokenizer;
import com.dataspec.reverseimport.repository.FieldSourceRepository;
import com.dataspec.security.context.DataSpecSecurityContext;
import com.dataspec.security.model.ApiTokenPrincipal;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 标准字段服务测试
 */
class FieldServiceImplTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @AfterEach
    void tearDown() {
        DataSpecSecurityContext.clear();
    }

    @Test
    void create_defaultsPersonalMetadata() {
        FieldRepository repository = mock(FieldRepository.class);
        when(repository.existsByNameInProject("mobile_no", 1L)).thenReturn(false);
        FieldServiceImpl service = service(repository, mock(StandardChangeLogService.class));

        Field field = new Field();
        field.setProjectId(1L);
        field.setName("mobile_no");
        field.setDataType("varchar(20)");

        Field created = service.create(field);

        assertTrue(created.getNullable());
        assertFalse(created.getSensitive());
        assertEquals("enabled", created.getStatus());
        verify(repository).insert(created);
    }

    @Test
    void create_rejectsInvalidStatus() {
        FieldRepository repository = mock(FieldRepository.class);
        FieldServiceImpl service = service(repository, mock(StandardChangeLogService.class));

        Field field = new Field();
        field.setProjectId(1L);
        field.setName("mobile_no");
        field.setDataType("varchar(20)");
        field.setStatus("archived");

        assertThrows(BizException.class, () -> service.create(field));
        verify(repository, never()).insert(any());
    }

    @Test
    void create_acceptsDraftStatus() {
        FieldRepository repository = mock(FieldRepository.class);
        when(repository.existsByNameInProject("draft_mobile_no", 1L)).thenReturn(false);
        FieldServiceImpl service = service(repository, mock(StandardChangeLogService.class));

        Field field = new Field();
        field.setProjectId(1L);
        field.setName("draft_mobile_no");
        field.setDataType("varchar(20)");
        field.setStatus("draft");

        Field created = service.create(field);

        assertEquals("draft", created.getStatus());
        verify(repository).insert(created);
    }

    @Test
    void create_preservesFieldFormatExampleText() throws Exception {
        FieldRepository repository = mock(FieldRepository.class);
        when(repository.existsByNameInProject("mobile_no", 1L)).thenReturn(false);
        FieldServiceImpl service = service(repository, mock(StandardChangeLogService.class));

        Field field = new Field();
        field.setProjectId(1L);
        field.setName("mobile_no");
        field.setDataType("varchar(20)");
        field.setFormatType(" mobile ");
        field.setFormatPattern(" ^1\\d{10}$ ");
        field.setFormatUnit(" string ");
        field.setFormatNullPolicy(" not_blank ");
        field.setValidExamplesJson("[\" 13800138000 \", \"\"]");
        field.setInvalidExamplesJson("[\"12345\"]");

        Field created = service.create(field);

        assertEquals("mobile", created.getFormatType());
        assertEquals("^1\\d{10}$", created.getFormatPattern());
        assertEquals("string", created.getFormatUnit());
        assertEquals("not_blank", created.getFormatNullPolicy());
        JsonNode validExamples = objectMapper.readTree(created.getValidExamplesJson());
        assertEquals(2, validExamples.size());
        assertEquals(" 13800138000 ", validExamples.get(0).asText());
        assertEquals("", validExamples.get(1).asText());
        assertEquals("12345", objectMapper.readTree(created.getInvalidExamplesJson()).get(0).asText());
        verify(repository).insert(created);
    }

    @Test
    void create_preservesAndNormalizesUsageContractText() {
        FieldRepository repository = mock(FieldRepository.class);
        when(repository.existsByNameInProject("amount_cent", 1L)).thenReturn(false);
        FieldServiceImpl service = service(repository, mock(StandardChangeLogService.class));

        Field field = new Field();
        field.setProjectId(1L);
        field.setName("amount_cent");
        field.setDataType("bigint");
        field.setPreferredUseCases("  统计订单实付金额  ");
        field.setAvoidWhen("  展示金额时不要直接输出分单位  ");
        field.setJoinHints("  orders.id = payments.order_id  ");
        field.setDefaultFilters("  status = 'PAID'  ");
        field.setAggregationHints("  sum(amount_cent) / 100  ");
        field.setReplacementGuidance("  展示层改用 amount_yuan  ");
        field.setMisuseExamples("  把 amount_cent 当元展示  ");

        Field created = service.create(field);

        assertEquals("统计订单实付金额", created.getPreferredUseCases());
        assertEquals("展示金额时不要直接输出分单位", created.getAvoidWhen());
        assertEquals("orders.id = payments.order_id", created.getJoinHints());
        assertEquals("status = 'PAID'", created.getDefaultFilters());
        assertEquals("sum(amount_cent) / 100", created.getAggregationHints());
        assertEquals("展示层改用 amount_yuan", created.getReplacementGuidance());
        assertEquals("把 amount_cent 当元展示", created.getMisuseExamples());
        verify(repository).insert(created);
    }

    @Test
    void create_rejectsSensitiveUsageContractText() {
        FieldRepository repository = mock(FieldRepository.class);
        when(repository.existsByNameInProject("debug_note", 1L)).thenReturn(false);
        FieldServiceImpl service = service(repository, mock(StandardChangeLogService.class));

        Field field = new Field();
        field.setProjectId(1L);
        field.setName("debug_note");
        field.setDataType("text");
        field.setAvoidWhen("不要在契约里记录 Authorization: Bearer raw.jwt 或 jdbc:postgresql://localhost/app");

        BizException ex = assertThrows(BizException.class, () -> service.create(field));

        assertTrue(ex.getMessage().contains("字段使用契约"));
        assertFalse(ex.getMessage().contains("raw.jwt"));
        assertFalse(ex.getMessage().contains("jdbc:postgresql://localhost/app"));
        verify(repository, never()).insert(any());
    }

    @Test
    void create_rejectsNonStringFormatExamples() {
        FieldRepository repository = mock(FieldRepository.class);
        when(repository.existsByNameInProject("mobile_no", 1L)).thenReturn(false);
        FieldServiceImpl service = service(repository, mock(StandardChangeLogService.class));

        Field field = new Field();
        field.setProjectId(1L);
        field.setName("mobile_no");
        field.setDataType("varchar(20)");
        field.setValidExamplesJson("[\"13800138000\", 123]");

        BizException ex = assertThrows(BizException.class, () -> service.create(field));

        assertTrue(ex.getMessage().contains("validExamplesJson"));
        verify(repository, never()).insert(any());
    }

    @Test
    void create_rejectsCrossProjectReplacementField() {
        FieldRepository repository = mock(FieldRepository.class);
        when(repository.existsByNameInProject("old_mobile_no", 1L)).thenReturn(false);
        Field replacement = new Field();
        replacement.setId(12L);
        replacement.setProjectId(2L);
        when(repository.findById(12L)).thenReturn(Optional.of(replacement));
        FieldServiceImpl service = service(repository, mock(StandardChangeLogService.class));

        Field field = new Field();
        field.setProjectId(1L);
        field.setName("old_mobile_no");
        field.setDataType("varchar(20)");
        field.setStatus("deprecated");
        field.setReplacementFieldId(12L);

        assertThrows(BizException.class, () -> service.create(field));
        verify(repository, never()).insert(any());
    }

    @Test
    void update_copiesPersonalMetadata() {
        FieldRepository repository = mock(FieldRepository.class);
        Field existing = new Field();
        existing.setId(9L);
        existing.setProjectId(1L);
        existing.setName("mobile_no");
        when(repository.findById(9L)).thenReturn(Optional.of(existing));
        Field replacement = new Field();
        replacement.setId(12L);
        replacement.setProjectId(1L);
        when(repository.findById(12L)).thenReturn(Optional.of(replacement));
        when(repository.existsByNameInProjectExcludeId("mobile_no", 1L, 9L)).thenReturn(false);
        FieldServiceImpl service = service(repository, mock(StandardChangeLogService.class));

        Field incoming = new Field();
        incoming.setName("mobile_no");
        incoming.setDisplayName("手机号");
        incoming.setDataType("varchar(20)");
        incoming.setNullable(false);
        incoming.setAliases("phone,mobile");
        incoming.setCategory("contact");
        incoming.setCodeSetId(10L);
        incoming.setSensitive(true);
        incoming.setStatus("deprecated");
        incoming.setExampleValue("13800138000");
        incoming.setReplacementFieldId(12L);
        incoming.setReplacementReason("历史字段，改用 user_mobile_no");
        incoming.setFormatType("mobile");
        incoming.setFormatPattern("^1\\d{10}$");
        incoming.setFormatUnit("string");
        incoming.setFormatPrecision("11 digits");
        incoming.setFormatTimezone("Asia/Shanghai");
        incoming.setFormatNullPolicy("not_blank");
        incoming.setValidExamplesJson("[\"13800138000\"]");
        incoming.setInvalidExamplesJson("[\"12345\"]");
        incoming.setFormatNotes("中国大陆手机号");

        Field updated = service.update(9L, incoming);

        assertEquals("phone,mobile", updated.getAliases());
        assertEquals("contact", updated.getCategory());
        assertEquals(10L, updated.getCodeSetId());
        assertTrue(updated.getSensitive());
        assertEquals("deprecated", updated.getStatus());
        assertEquals("13800138000", updated.getExampleValue());
        assertEquals(12L, updated.getReplacementFieldId());
        assertEquals("历史字段，改用 user_mobile_no", updated.getReplacementReason());
        assertEquals("mobile", updated.getFormatType());
        assertEquals("^1\\d{10}$", updated.getFormatPattern());
        assertEquals("string", updated.getFormatUnit());
        assertEquals("11 digits", updated.getFormatPrecision());
        assertEquals("Asia/Shanghai", updated.getFormatTimezone());
        assertEquals("not_blank", updated.getFormatNullPolicy());
        assertEquals("[\"13800138000\"]", updated.getValidExamplesJson());
        assertEquals("[\"12345\"]", updated.getInvalidExamplesJson());
        assertEquals("中国大陆手机号", updated.getFormatNotes());
        verify(repository).update(updated);
    }

    @Test
    void update_copiesUsageContractMetadata() {
        FieldRepository repository = mock(FieldRepository.class);
        Field existing = new Field();
        existing.setId(9L);
        existing.setProjectId(1L);
        existing.setName("amount_cent");
        when(repository.findById(9L)).thenReturn(Optional.of(existing));
        when(repository.existsByNameInProjectExcludeId("amount_cent", 1L, 9L)).thenReturn(false);
        FieldServiceImpl service = service(repository, mock(StandardChangeLogService.class));

        Field incoming = new Field();
        incoming.setName("amount_cent");
        incoming.setDataType("bigint");
        incoming.setNullable(false);
        incoming.setPreferredUseCases("统计订单实付金额");
        incoming.setAvoidWhen("展示金额时不要直接输出分单位");
        incoming.setJoinHints("orders.id = payments.order_id");
        incoming.setDefaultFilters("status = 'PAID'");
        incoming.setAggregationHints("sum(amount_cent) / 100");
        incoming.setReplacementGuidance("展示层改用 amount_yuan");
        incoming.setMisuseExamples("把 amount_cent 当元展示");

        Field updated = service.update(9L, incoming);

        assertEquals("统计订单实付金额", updated.getPreferredUseCases());
        assertEquals("展示金额时不要直接输出分单位", updated.getAvoidWhen());
        assertEquals("orders.id = payments.order_id", updated.getJoinHints());
        assertEquals("status = 'PAID'", updated.getDefaultFilters());
        assertEquals("sum(amount_cent) / 100", updated.getAggregationHints());
        assertEquals("展示层改用 amount_yuan", updated.getReplacementGuidance());
        assertEquals("把 amount_cent 当元展示", updated.getMisuseExamples());
        verify(repository).update(updated);
    }

    @Test
    void create_normalizesSemanticTranslationMetadata() throws Exception {
        FieldRepository repository = mock(FieldRepository.class);
        when(repository.existsByNameInProject("amount_cent", 1L)).thenReturn(false);
        FieldServiceImpl service = service(repository, mock(StandardChangeLogService.class));

        Field field = new Field();
        field.setProjectId(1L);
        field.setName("amount_cent");
        field.setDataType("bigint");
        field.setLocalizedNamesJson(" { \"zh\": \"订单金额\", \"en\": \"amount\" } ");
        field.setPreferredEnglishName(" amount_cent ");
        field.setForbiddenTranslationsJson("[\"amount\"]");
        field.setTranslationAliasesJson("[\"paid amount\", \"支付金额\"]");
        field.setTranslationConfidence(" HIGH ");
        field.setTranslationNotes(" 统一翻译为 amount_cent，避免 amount_yuan 混用 ");
        field.setSemanticSummary(" 金额以分存储，展示前需要换算 ");

        Field created = service.create(field);

        JsonNode localizedNames = objectMapper.readTree(created.getLocalizedNamesJson());
        assertEquals("订单金额", localizedNames.path("zh").asText());
        assertEquals("amount_cent", created.getPreferredEnglishName());
        assertEquals("amount", objectMapper.readTree(created.getForbiddenTranslationsJson()).get(0).asText());
        assertEquals("paid amount", objectMapper.readTree(created.getTranslationAliasesJson()).get(0).asText());
        assertEquals("high", created.getTranslationConfidence());
        assertEquals("统一翻译为 amount_cent，避免 amount_yuan 混用", created.getTranslationNotes());
        assertEquals("金额以分存储，展示前需要换算", created.getSemanticSummary());
        verify(repository).insert(created);
    }

    @Test
    void create_rejectsSensitiveSemanticTranslationJsonWithoutEchoingSecret() {
        FieldRepository repository = mock(FieldRepository.class);
        when(repository.existsByNameInProject("amount_cent", 1L)).thenReturn(false);
        when(repository.existsByNameInProject("payment_status", 1L)).thenReturn(false);
        FieldServiceImpl service = service(repository, mock(StandardChangeLogService.class));

        Field forbidden = new Field();
        forbidden.setProjectId(1L);
        forbidden.setName("amount_cent");
        forbidden.setDataType("bigint");
        forbidden.setForbiddenTranslationsJson("[\"jdbc:postgresql://localhost/app?password=raw-secret\"]");

        BizException forbiddenEx = assertThrows(BizException.class, () -> service.create(forbidden));

        assertTrue(forbiddenEx.getMessage().contains("forbiddenTranslationsJson"));
        assertFalse(forbiddenEx.getMessage().contains("raw-secret"));
        assertFalse(forbiddenEx.getMessage().contains("jdbc:postgresql://localhost/app"));

        Field aliases = new Field();
        aliases.setProjectId(1L);
        aliases.setName("payment_status");
        aliases.setDataType("varchar(20)");
        aliases.setTranslationAliasesJson("[\"Authorization: Bearer raw.jwt\"]");

        BizException aliasEx = assertThrows(BizException.class, () -> service.create(aliases));

        assertTrue(aliasEx.getMessage().contains("translationAliasesJson"));
        assertFalse(aliasEx.getMessage().contains("raw.jwt"));
        verify(repository, never()).insert(any());
    }

    @Test
    void create_rejectsSensitiveTranslationConfidenceWithoutEchoingSecret() {
        FieldRepository repository = mock(FieldRepository.class);
        when(repository.existsByNameInProject("amount_cent", 1L)).thenReturn(false);
        FieldServiceImpl service = service(repository, mock(StandardChangeLogService.class));

        Field field = new Field();
        field.setProjectId(1L);
        field.setName("amount_cent");
        field.setDataType("bigint");
        field.setTranslationConfidence("token=raw-confidence-secret");

        BizException ex = assertThrows(BizException.class, () -> service.create(field));

        assertTrue(ex.getMessage().contains("translationConfidence 包含敏感连接或凭据信息"));
        assertFalse(ex.getMessage().contains("raw-confidence-secret"));
        verify(repository, never()).insert(any());
    }

    @Test
    void update_preservesSemanticMetadataWhenOrdinaryUpdateOmitsAdditiveFields() {
        FieldRepository repository = mock(FieldRepository.class);
        Field existing = new Field();
        existing.setId(9L);
        existing.setProjectId(1L);
        existing.setName("amount_cent");
        existing.setDataType("bigint");
        existing.setLocalizedNamesJson("{\"zh\":\"订单金额\"}");
        existing.setPreferredEnglishName("amount_cent");
        existing.setForbiddenTranslationsJson("[\"amount\"]");
        existing.setTranslationAliasesJson("[\"paid amount\"]");
        existing.setTranslationConfidence("high");
        existing.setTranslationNotes("保留命名取舍");
        existing.setSemanticSummary("金额以分存储");
        when(repository.findById(9L)).thenReturn(Optional.of(existing));
        when(repository.existsByNameInProjectExcludeId("amount_cent", 1L, 9L)).thenReturn(false);
        FieldServiceImpl service = service(repository, mock(StandardChangeLogService.class));

        Field incoming = new Field();
        incoming.setName("amount_cent");
        incoming.setDataType("bigint");
        incoming.setNullable(false);
        incoming.setComment("订单金额字段");

        Field updated = service.update(9L, incoming);

        assertEquals("{\"zh\":\"订单金额\"}", updated.getLocalizedNamesJson());
        assertEquals("amount_cent", updated.getPreferredEnglishName());
        assertEquals("[\"amount\"]", updated.getForbiddenTranslationsJson());
        assertEquals("[\"paid amount\"]", updated.getTranslationAliasesJson());
        assertEquals("high", updated.getTranslationConfidence());
        assertEquals("保留命名取舍", updated.getTranslationNotes());
        assertEquals("金额以分存储", updated.getSemanticSummary());
        verify(repository).update(updated);
    }

    @Test
    void usageContractFields_allowNullUpdatesForClearingSavedContract() throws Exception {
        for (String fieldName : List.of(
                "preferredUseCases",
                "avoidWhen",
                "joinHints",
                "defaultFilters",
                "aggregationHints",
                "replacementGuidance",
                "misuseExamples")) {
            TableField tableField = Field.class.getDeclaredField(fieldName).getAnnotation(TableField.class);

            assertNotNull(tableField, fieldName + " 应显式映射数据库列");
            assertEquals(
                    FieldStrategy.ALWAYS,
                    tableField.updateStrategy(),
                    fieldName + " 必须允许把已有契约清空为 NULL，否则 MyBatis-Plus updateById 会跳过 null");
        }
    }

    @Test
    void update_recordsLifecycleReplacementInChangeLog() {
        FieldRepository repository = mock(FieldRepository.class);
        StandardChangeLogService changeLogService = mock(StandardChangeLogService.class);
        Field existing = new Field();
        existing.setId(9L);
        existing.setProjectId(1L);
        existing.setName("old_mobile_no");
        existing.setDataType("varchar(20)");
        existing.setStatus("enabled");
        when(repository.findById(9L)).thenReturn(Optional.of(existing));
        Field replacement = new Field();
        replacement.setId(12L);
        replacement.setProjectId(1L);
        when(repository.findById(12L)).thenReturn(Optional.of(replacement));
        when(repository.existsByNameInProjectExcludeId("old_mobile_no", 1L, 9L)).thenReturn(false);
        when(changeLogService.snapshot(any(Field.class))).thenAnswer(invocation -> {
            Field field = invocation.getArgument(0);
            return field.getStatus() + "|" + field.getReplacementFieldId()
                    + "|" + field.getReplacementReason();
        });
        FieldServiceImpl service = service(repository, changeLogService);

        Field incoming = new Field();
        incoming.setName("old_mobile_no");
        incoming.setDataType("varchar(20)");
        incoming.setStatus("deprecated");
        incoming.setNullable(true);
        incoming.setReplacementFieldId(12L);
        incoming.setReplacementReason("历史兼容字段，改用 mobile_no");

        service.update(9L, incoming);

        verify(changeLogService).recordChange(
                1L,
                "field",
                9L,
                "update",
                "enabled|null|null",
                "deprecated|12|历史兼容字段，改用 mobile_no");
    }

    @Test
    void update_recordsBeforeAndAfterChangeLog() {
        FieldRepository repository = mock(FieldRepository.class);
        StandardChangeLogService changeLogService = mock(StandardChangeLogService.class);
        Field existing = new Field();
        existing.setId(9L);
        existing.setProjectId(1L);
        existing.setName("mobile_no");
        existing.setDataType("varchar(20)");
        existing.setAliases("phone");
        when(repository.findById(9L)).thenReturn(Optional.of(existing));
        when(repository.existsByNameInProjectExcludeId("mobile_no", 1L, 9L)).thenReturn(false);
        when(changeLogService.snapshot(any(Field.class))).thenAnswer(invocation -> {
            Field field = invocation.getArgument(0);
            return field.getDataType() + "|" + field.getAliases();
        });
        FieldServiceImpl service = service(repository, changeLogService);

        Field incoming = new Field();
        incoming.setName("mobile_no");
        incoming.setDataType("varchar(30)");
        incoming.setAliases("phone,mobile");
        incoming.setNullable(true);

        service.update(9L, incoming);

        verify(changeLogService).recordChange(
                1L,
                "field",
                9L,
                "update",
                "varchar(20)|phone",
                "varchar(30)|phone,mobile");
    }

    @Test
    void groupingSummary_groupsDomainCategoryTagAndUngroupedFields() {
        FieldRepository repository = mock(FieldRepository.class);
        Field mobile = field("mobile_no", "手机号", "varchar(20)", "用户手机号", "phone,mobile", "enabled");
        mobile.setDomainId(10L);
        mobile.setCategory("contact");
        mobile.setTags("pii, customer");
        Field email = field("email", "邮箱", "varchar(128)", "邮箱", "mail", "enabled");
        email.setDomainId(10L);
        email.setCategory("contact");
        email.setTags("pii");
        Field orderNo = field("order_no", "订单号", "varchar(64)", "订单号", "order", "enabled");
        orderNo.setCategory("order");
        Field raw = field("raw_payload", "原始报文", "jsonb", "原始报文", "", "enabled");
        when(repository.findAllByProjectId(1L)).thenReturn(List.of(mobile, email, orderNo, raw));
        FieldServiceImpl service = service(repository, mock(StandardChangeLogService.class));

        FieldGroupSummary summary = service.groupSummary(1L);

        assertEquals(4, summary.totalFieldCount());
        assertEquals(1, summary.ungroupedFieldCount());
        assertTrue(summary.groups().stream().anyMatch(group ->
                "domain".equals(group.groupType())
                        && "10".equals(group.groupKey())
                        && group.fieldCount() == 2
                        && group.sampleFields().contains("mobile_no")));
        assertTrue(summary.groups().stream().anyMatch(group ->
                "category".equals(group.groupType())
                        && "contact".equals(group.groupKey())
                        && group.fieldCount() == 2));
        assertTrue(summary.groups().stream().anyMatch(group ->
                "tag".equals(group.groupType())
                        && "pii".equals(group.groupKey())
                        && group.fieldCount() == 2));
        assertTrue(summary.groups().stream().anyMatch(group ->
                "ungrouped".equals(group.groupType())
                        && group.ungrouped()
                        && group.sampleFields().contains("raw_payload")));
    }

    @Test
    void batchUpdateGrouping_updatesExplicitFieldsAndRecordsChangeLogs() {
        FieldRepository repository = mock(FieldRepository.class);
        StandardChangeLogService changeLogService = mock(StandardChangeLogService.class);
        Field mobile = field("mobile_no", "手机号", "varchar(20)", "用户手机号", "phone", "enabled");
        mobile.setId(1L);
        mobile.setProjectId(1L);
        mobile.setCategory("old");
        mobile.setTags("legacy");
        Field email = field("email", "邮箱", "varchar(128)", "邮箱", "mail", "enabled");
        email.setId(2L);
        email.setProjectId(1L);
        when(repository.findById(1L)).thenReturn(Optional.of(mobile));
        when(repository.findById(2L)).thenReturn(Optional.of(email));
        when(changeLogService.snapshot(any(Field.class))).thenAnswer(invocation -> {
            Field field = invocation.getArgument(0);
            return field.getName() + "|" + field.getDomainId() + "|" + field.getCategory() + "|" + field.getTags();
        });
        FieldServiceImpl service = service(repository, changeLogService);

        FieldGroupingBatchUpdateResult result = service.batchUpdateGrouping(new FieldGroupingBatchUpdateReq(
                1L,
                List.of(1L, 2L),
                Map.of(
                        "domainId", 10,
                        "category", "contact",
                        "tags", "pii, customer"
                )));

        assertEquals(2, result.updatedCount());
        assertEquals(10L, mobile.getDomainId());
        assertEquals("contact", mobile.getCategory());
        assertEquals("customer,pii", mobile.getTags());
        assertEquals("contact", email.getCategory());
        verify(repository).update(mobile);
        verify(repository).update(email);
        verify(changeLogService).recordChange(eq(1L), eq("field"), eq(1L), eq("update"),
                eq("mobile_no|null|old|legacy"), eq("mobile_no|10|contact|customer,pii"));
        verify(changeLogService).recordChange(eq(1L), eq("field"), eq(2L), eq("update"),
                eq("email|null|null|null"), eq("email|10|contact|customer,pii"));
    }

    @Test
    void batchUpdateGrouping_rejectsCrossProjectWithoutPartialUpdate() {
        FieldRepository repository = mock(FieldRepository.class);
        Field own = field("mobile_no", "手机号", "varchar(20)", "用户手机号", "phone", "enabled");
        own.setId(1L);
        own.setProjectId(1L);
        Field foreign = field("email", "邮箱", "varchar(128)", "邮箱", "mail", "enabled");
        foreign.setId(2L);
        foreign.setProjectId(2L);
        when(repository.findById(1L)).thenReturn(Optional.of(own));
        when(repository.findById(2L)).thenReturn(Optional.of(foreign));
        FieldServiceImpl service = service(repository, mock(StandardChangeLogService.class));

        assertThrows(BizException.class, () -> service.batchUpdateGrouping(new FieldGroupingBatchUpdateReq(
                1L,
                List.of(1L, 2L),
                Map.of("category", "contact"))));
        verify(repository, never()).update(any());
    }

    @Test
    void batchUpdateGrouping_canClearGroupingFields() {
        FieldRepository repository = mock(FieldRepository.class);
        Field mobile = field("mobile_no", "手机号", "varchar(20)", "用户手机号", "phone", "enabled");
        mobile.setId(1L);
        mobile.setProjectId(1L);
        mobile.setDomainId(10L);
        mobile.setCategory("contact");
        mobile.setTags("pii");
        when(repository.findById(1L)).thenReturn(Optional.of(mobile));
        FieldServiceImpl service = service(repository, mock(StandardChangeLogService.class));

        service.batchUpdateGrouping(new FieldGroupingBatchUpdateReq(
                1L,
                List.of(1L),
                Map.of(
                        "domainId", "",
                        "category", "",
                        "tags", ""
                )));

        assertNull(mobile.getDomainId());
        assertNull(mobile.getCategory());
        assertNull(mobile.getTags());
    }

    @Test
    void batchUpdateGrouping_deduplicatesFieldIds() {
        FieldRepository repository = mock(FieldRepository.class);
        Field mobile = field("mobile_no", "手机号", "varchar(20)", "用户手机号", "phone", "enabled");
        mobile.setId(1L);
        mobile.setProjectId(1L);
        when(repository.findById(1L)).thenReturn(Optional.of(mobile));
        FieldServiceImpl service = service(repository, mock(StandardChangeLogService.class));

        FieldGroupingBatchUpdateResult result = service.batchUpdateGrouping(new FieldGroupingBatchUpdateReq(
                1L,
                List.of(1L, 1L),
                Map.of("category", "contact")));

        assertEquals(2, result.requestedCount());
        assertEquals(1, result.updatedCount());
        verify(repository, times(1)).findById(1L);
        verify(repository, times(1)).update(mobile);
    }

    @Test
    void batchUpdateGrouping_rejectsInvalidFieldIdsBeforeRepositoryLookup() {
        FieldRepository repository = mock(FieldRepository.class);
        FieldServiceImpl service = service(repository, mock(StandardChangeLogService.class));

        assertThrows(BizException.class, () -> service.batchUpdateGrouping(new FieldGroupingBatchUpdateReq(
                1L,
                List.of(0L),
                Map.of("category", "contact"))));

        verify(repository, never()).findById(any());
        verify(repository, never()).update(any());
    }

    @Test
    void bulkUpdatePreview_reportsNormalizedChangesWithoutWriting() {
        FieldRepository repository = mock(FieldRepository.class);
        Field mobile = field("mobile_no", "手机号", "varchar(20)", "用户手机号", "phone", "enabled");
        mobile.setId(1L);
        mobile.setProjectId(1L);
        mobile.setCategory("old");
        mobile.setTags("legacy");
        mobile.setSensitive(false);
        when(repository.findById(1L)).thenReturn(Optional.of(mobile));
        StandardChangeLogService changeLogService = mock(StandardChangeLogService.class);
        FieldServiceImpl service = service(repository, changeLogService);

        FieldBulkUpdatePreview preview = service.previewBulkUpdate(new FieldBulkUpdateReq(
                1L,
                List.of(1L),
                Map.of(
                        "status", "deprecated",
                        "category", "contact",
                        "tags", "pii, customer",
                        "sensitive", true
                )));

        assertEquals(1, preview.requestedCount());
        assertEquals(1, preview.changedCount());
        assertEquals(0, preview.unchangedCount());
        assertEquals(4, preview.items().getFirst().changes().size());
        assertTrue(preview.items().getFirst().changes().stream()
                .anyMatch(change -> "tags".equals(change.attribute())
                        && "legacy".equals(change.beforeValue())
                        && "customer,pii".equals(change.afterValue())));
        verify(repository, never()).update(any());
        verify(changeLogService, never()).recordChange(anyLong(), anyString(), anyLong(), anyString(), any(), any());
    }

    @Test
    void bulkUpdatePreview_deduplicatesFieldIdsInCounts() {
        FieldRepository repository = mock(FieldRepository.class);
        Field mobile = field("mobile_no", "手机号", "varchar(20)", "用户手机号", "phone", "enabled");
        mobile.setId(1L);
        mobile.setProjectId(1L);
        when(repository.findById(1L)).thenReturn(Optional.of(mobile));
        FieldServiceImpl service = service(repository, mock(StandardChangeLogService.class));

        FieldBulkUpdatePreview preview = service.previewBulkUpdate(new FieldBulkUpdateReq(
                1L,
                List.of(1L, 1L),
                Map.of("status", "deprecated")));

        assertEquals(1, preview.requestedCount());
        assertEquals(1, preview.items().size());
        verify(repository, times(1)).findById(1L);
    }

    @Test
    void bulkUpdateFields_updatesChangedFieldsAndSkipsNoopLogs() {
        FieldRepository repository = mock(FieldRepository.class);
        StandardChangeLogService changeLogService = mock(StandardChangeLogService.class);
        Field mobile = field("mobile_no", "手机号", "varchar(20)", "用户手机号", "phone", "enabled");
        mobile.setId(1L);
        mobile.setProjectId(1L);
        mobile.setCategory("old");
        Field email = field("email", "邮箱", "varchar(128)", "邮箱", "mail", "enabled");
        email.setId(2L);
        email.setProjectId(1L);
        email.setCategory("contact");
        when(repository.findById(1L)).thenReturn(Optional.of(mobile));
        when(repository.findById(2L)).thenReturn(Optional.of(email));
        when(changeLogService.snapshot(any(Field.class))).thenAnswer(invocation -> {
            Field field = invocation.getArgument(0);
            return field.getName() + "|" + field.getCategory();
        });
        FieldServiceImpl service = service(repository, changeLogService);

        FieldBulkUpdateResult result = service.bulkUpdateFields(new FieldBulkUpdateReq(
                1L,
                List.of(1L, 2L),
                Map.of("category", "contact")));

        assertEquals(2, result.requestedCount());
        assertEquals(1, result.updatedCount());
        assertEquals(1, result.unchangedCount());
        assertEquals("contact", mobile.getCategory());
        verify(repository).update(mobile);
        verify(repository, never()).update(email);
        verify(changeLogService).recordChange(eq(1L), eq("field"), eq(1L), eq("update"),
                eq("mobile_no|old"), eq("mobile_no|contact"));
    }

    @Test
    void bulkUpdatePreview_rejectsUnsupportedKeysBeforeRepositoryLookup() {
        FieldRepository repository = mock(FieldRepository.class);
        FieldServiceImpl service = service(repository, mock(StandardChangeLogService.class));

        assertThrows(BizException.class, () -> service.previewBulkUpdate(new FieldBulkUpdateReq(
                1L,
                List.of(1L),
                Map.of("name", "bad_name"))));

        verify(repository, never()).findById(any());
        verify(repository, never()).update(any());
    }

    @Test
    void bulkUpdateFields_rejectsCrossProjectWithoutPartialUpdate() {
        FieldRepository repository = mock(FieldRepository.class);
        Field own = field("mobile_no", "手机号", "varchar(20)", "用户手机号", "phone", "enabled");
        own.setId(1L);
        own.setProjectId(1L);
        Field foreign = field("email", "邮箱", "varchar(128)", "邮箱", "mail", "enabled");
        foreign.setId(2L);
        foreign.setProjectId(2L);
        when(repository.findById(1L)).thenReturn(Optional.of(own));
        when(repository.findById(2L)).thenReturn(Optional.of(foreign));
        FieldServiceImpl service = service(repository, mock(StandardChangeLogService.class));

        assertThrows(BizException.class, () -> service.bulkUpdateFields(new FieldBulkUpdateReq(
                1L,
                List.of(1L, 2L),
                Map.of("status", "deprecated"))));

        verify(repository, never()).update(any());
    }

    @Test
    void undoFieldChange_restoresBeforeSnapshotAndRecordsUndoLog() throws Exception {
        FieldRepository repository = mock(FieldRepository.class);
        StandardChangeLogService changeLogService = mock(StandardChangeLogService.class);
        Field existing = field("mobile_no", "手机号", "varchar(20)", "用户手机号", "phone", "deprecated");
        existing.setId(1L);
        existing.setProjectId(1L);
        existing.setCategory("new");
        Field before = field("mobile_no", "手机号", "varchar(20)", "用户手机号", "phone", "enabled");
        before.setId(1L);
        before.setProjectId(1L);
        before.setCategory("old");
        StandardChangeLog log = fieldLog(50L, 1L, 1L, "update", objectMapper.writeValueAsString(before));
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(repository.existsByNameInProjectExcludeId("mobile_no", 1L, 1L)).thenReturn(false);
        when(changeLogService.getById(50L)).thenReturn(log);
        when(changeLogService.snapshot(any(Field.class))).thenAnswer(invocation ->
                objectMapper.writeValueAsString(invocation.getArgument(0)));
        FieldServiceImpl service = service(repository, changeLogService);

        FieldChangeUndoResult result = service.undoFieldChange(1L, 50L);

        assertEquals(1L, result.fieldId());
        assertEquals(50L, result.logId());
        assertEquals("enabled", existing.getStatus());
        assertEquals("old", existing.getCategory());
        verify(repository).update(existing);
        verify(changeLogService).recordChange(eq(1L), eq("field"), eq(1L), eq("undo"), anyString(), anyString());
    }

    @Test
    void undoFieldChange_rejectsNameConflictWithoutUpdating() throws Exception {
        FieldRepository repository = mock(FieldRepository.class);
        StandardChangeLogService changeLogService = mock(StandardChangeLogService.class);
        Field existing = field("mobile_no", "手机号", "varchar(20)", "用户手机号", "phone", "enabled");
        existing.setId(1L);
        existing.setProjectId(1L);
        Field before = field("email", "邮箱", "varchar(128)", "邮箱", "mail", "enabled");
        before.setId(1L);
        before.setProjectId(1L);
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(repository.existsByNameInProjectExcludeId("email", 1L, 1L)).thenReturn(true);
        when(changeLogService.getById(50L))
                .thenReturn(fieldLog(50L, 1L, 1L, "update", objectMapper.writeValueAsString(before)));
        FieldServiceImpl service = service(repository, changeLogService);

        assertThrows(BizException.class, () -> service.undoFieldChange(1L, 50L));

        verify(repository, never()).update(any());
        verify(changeLogService, never()).recordChange(anyLong(), anyString(), anyLong(), anyString(), any(), any());
    }

    @Test
    void undoFieldChange_rejectsMismatchedLogTarget() throws Exception {
        FieldRepository repository = mock(FieldRepository.class);
        StandardChangeLogService changeLogService = mock(StandardChangeLogService.class);
        Field existing = field("mobile_no", "手机号", "varchar(20)", "用户手机号", "phone", "enabled");
        existing.setId(1L);
        existing.setProjectId(1L);
        Field before = field("mobile_no", "手机号", "varchar(20)", "用户手机号", "phone", "enabled");
        before.setId(2L);
        before.setProjectId(1L);
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(changeLogService.getById(50L))
                .thenReturn(fieldLog(50L, 1L, 2L, "update", objectMapper.writeValueAsString(before)));
        FieldServiceImpl service = service(repository, changeLogService);

        assertThrows(BizException.class, () -> service.undoFieldChange(1L, 50L));

        verify(repository, never()).update(any());
    }

    @Test
    void undoFieldChange_rejectsCrossProjectLogWithoutUpdating() throws Exception {
        FieldRepository repository = mock(FieldRepository.class);
        StandardChangeLogService changeLogService = mock(StandardChangeLogService.class);
        Field existing = field("mobile_no", "手机号", "varchar(20)", "用户手机号", "phone", "enabled");
        existing.setId(1L);
        existing.setProjectId(1L);
        Field before = field("mobile_no", "手机号", "varchar(20)", "用户手机号", "phone", "enabled");
        before.setId(1L);
        before.setProjectId(2L);
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(changeLogService.getById(50L))
                .thenReturn(fieldLog(50L, 2L, 1L, "update", objectMapper.writeValueAsString(before)));
        FieldServiceImpl service = service(repository, changeLogService);

        assertThrows(BizException.class, () -> service.undoFieldChange(1L, 50L));

        verify(repository, never()).update(any());
    }

    @Test
    void undoFieldChange_rejectsMissingBeforeJsonWithoutUpdating() {
        FieldRepository repository = mock(FieldRepository.class);
        StandardChangeLogService changeLogService = mock(StandardChangeLogService.class);
        Field existing = field("mobile_no", "手机号", "varchar(20)", "用户手机号", "phone", "enabled");
        existing.setId(1L);
        existing.setProjectId(1L);
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(changeLogService.getById(50L)).thenReturn(fieldLog(50L, 1L, 1L, "update", null));
        FieldServiceImpl service = service(repository, changeLogService);

        assertThrows(BizException.class, () -> service.undoFieldChange(1L, 50L));

        verify(repository, never()).update(any());
    }

    @Test
    void undoFieldChange_rejectsMismatchedSnapshotWithoutUpdating() throws Exception {
        FieldRepository repository = mock(FieldRepository.class);
        StandardChangeLogService changeLogService = mock(StandardChangeLogService.class);
        Field existing = field("mobile_no", "手机号", "varchar(20)", "用户手机号", "phone", "enabled");
        existing.setId(1L);
        existing.setProjectId(1L);
        Field before = field("email", "邮箱", "varchar(128)", "邮箱", "mail", "enabled");
        before.setId(2L);
        before.setProjectId(1L);
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(changeLogService.getById(50L))
                .thenReturn(fieldLog(50L, 1L, 1L, "update", objectMapper.writeValueAsString(before)));
        FieldServiceImpl service = service(repository, changeLogService);

        assertThrows(BizException.class, () -> service.undoFieldChange(1L, 50L));

        verify(repository, never()).update(any());
    }

    @Test
    void suggest_matchesAliasAndChineseDisplayName() {
        FieldRepository repository = mock(FieldRepository.class);
        when(repository.findAllByProjectId(1L)).thenReturn(List.of(
                field("mobile_no", "手机号", "varchar(20)", "用户手机号", "phone,mobile,tel", "enabled"),
                field("amount_cent", "金额（分）", "bigint", "支付金额，以分存储", "amount,pay_amount", "enabled")
        ));
        FieldServiceImpl service = service(repository, mock(StandardChangeLogService.class));

        List<FieldSuggestion> suggestions = service.suggest(1L, "用户手机号", 5);

        assertFalse(suggestions.isEmpty());
        FieldSuggestion first = suggestions.getFirst();
        assertTrue(first.existing());
        assertEquals("mobile_no", first.recommendedName());
        assertEquals("mobile_no", first.field().getName());
        assertTrue(first.score() > 0);
        assertTrue(first.matchReason().contains("显示名")
                || first.matchReason().contains("注释")
                || first.matchReason().contains("语义词"));
    }

    @Test
    void search_matchesKeywordAliasesAndReturnsAiReadableFields() {
        FieldRepository repository = mock(FieldRepository.class);
        Field mobile = field("mobile_no", "联系电话", "varchar(20)", "用户联系号码", "phone,mobile,tel", "enabled");
        mobile.setId(1L);
        mobile.setCategory("contact");
        mobile.setTags("pii,customer");
        mobile.setSensitive(true);
        Field amount = field("amount_cent", "金额（分）", "bigint", "支付金额，以分存储", "amount,pay_amount", "enabled");
        amount.setId(2L);
        amount.setCategory("money");
        when(repository.findAllByProjectId(1L)).thenReturn(List.of(amount, mobile));
        FieldServiceImpl service = service(repository, mock(StandardChangeLogService.class));

        FieldSearchResult result = service.search(new FieldSearchReq(
                1L, "用户sjh", null, null, null, null, null, 10));

        assertEquals(1L, result.projectId());
        assertEquals("用户sjh", result.query());
        assertEquals(1, result.items().size());
        assertEquals("mobile_no", result.items().getFirst().field().getName());
        assertTrue(result.items().getFirst().score() > 0);
        assertFalse(result.items().getFirst().matchReasons().isEmpty());
        assertTrue(result.items().getFirst().recommendedUse().contains("敏感字段"));
        assertFalse(result.items().getFirst().nextActions().isEmpty());
        assertEquals(1, result.summary().matchedCount());

        JsonNode evidence = objectMapper.valueToTree(result.items().getFirst()).path("evidence");
        assertTrue(evidence.isArray());
        assertFalse(evidence.isEmpty());
        assertEquals("FIELD", evidence.get(0).path("sourceType").asText());
        assertEquals(1L, evidence.get(0).path("sourceId").asLong());
        assertTrue(evidence.get(0).path("matchReason").asText().contains("语义词")
                || evidence.get(0).path("matchReason").asText().contains("显示名")
                || evidence.get(0).path("matchReason").asText().contains("注释"));
        assertTrue(evidence.get(0).path("confidence").asInt() > 0);
        assertFalse(evidence.get(0).path("docsRef").asText().isBlank());
    }

    @Test
    void search_returnsUsageContractSummaryAndConfirmationActionForAvoidMatch() {
        FieldRepository repository = mock(FieldRepository.class);
        Field amount = field("amount_cent", "订单金额", "bigint", "订单金额以分存储", "amount", "enabled");
        amount.setId(20L);
        amount.setPreferredUseCases("统计订单实付金额");
        amount.setAvoidWhen("展示金额时不要直接输出分单位");
        amount.setAggregationHints("sum(amount_cent) / 100");
        amount.setMisuseExamples("把 amount_cent 当元展示");
        when(repository.findAllByProjectId(1L)).thenReturn(List.of(amount));
        FieldServiceImpl service = service(repository, mock(StandardChangeLogService.class));

        FieldSearchResult result = service.search(new FieldSearchReq(
                1L, "展示订单金额", null, null, null, null, null, 10));

        assertEquals(1, result.items().size());
        FieldSearchItem item = result.items().getFirst();
        assertTrue(item.usageContractSummary().stream().anyMatch(text -> text.contains("统计订单实付金额")));
        assertTrue(item.usageContractSummary().stream().anyMatch(text -> text.contains("不要直接输出分单位")));
        assertTrue(item.nextActions().stream().anyMatch(text -> text.contains("字段使用契约") && text.contains("人工确认")));
        assertFalse(item.recommendedUse().contains("直接安全"));
    }

    @Test
    void suggest_usesBusinessGlossaryCanonicalFieldAndReason() {
        FieldRepository repository = mock(FieldRepository.class);
        BusinessGlossaryService glossaryService = mock(BusinessGlossaryService.class);
        Field mobile = field("mobile_no", "手机号", "varchar(20)", "联系人号码", "", "enabled");
        mobile.setId(10L);
        when(repository.findAllByProjectId(1L)).thenReturn(List.of(mobile));
        when(glossaryService.match(1L, "会员手机号")).thenReturn(List.of(
                new GlossaryMatch(3L, "手机号", "手机号", "TERM", 120, 10L, "mobile_no", Set.of("mobile_no"), false,
                        "术语表：手机号 -> mobile_no")
        ));
        FieldServiceImpl service = service(repository, mock(FieldSourceRepository.class), mock(StandardChangeLogService.class), glossaryService);

        List<FieldSuggestion> suggestions = service.suggest(1L, "会员手机号", 5);

        assertEquals("mobile_no", suggestions.getFirst().recommendedName());
        assertTrue(suggestions.getFirst().matchReason().contains("术语表"));
        assertEquals(QueryTokenResolutionStatus.RESOLVED,
                suggestions.getFirst().queryTokens().stream()
                        .filter(token -> "手机号".equals(token.normalizedToken()))
                        .findFirst()
                        .orElseThrow()
                        .resolutionStatus());
        assertTrue(suggestions.getFirst().evidence().stream().anyMatch(trace ->
                "BUSINESS_GLOSSARY".equals(trace.sourceType())
                        && "GLOSSARY_LONGEST_MATCH".equals(trace.ruleCode())));
    }

    @Test
    void search_usesBusinessGlossaryExpandedTerms() {
        FieldRepository repository = mock(FieldRepository.class);
        BusinessGlossaryService glossaryService = mock(BusinessGlossaryService.class);
        Field amount = field("amount_cent", "支付金额", "bigint", "金额以分存储", "", "enabled");
        amount.setId(20L);
        when(repository.findAllByProjectId(1L)).thenReturn(List.of(amount));
        when(glossaryService.match(1L, "订单费用")).thenReturn(List.of(
                new GlossaryMatch(4L, "费用", "费用", "SYNONYM", 116, 20L, "amount_cent", Set.of("amount_cent"), false,
                        "术语表：费用 -> amount_cent")
        ));
        FieldServiceImpl service = service(repository, mock(FieldSourceRepository.class), mock(StandardChangeLogService.class), glossaryService);

        FieldSearchResult result = service.search(new FieldSearchReq(
                1L, "订单费用", null, null, null, null, null, 10));

        assertEquals("amount_cent", result.items().getFirst().field().getName());
        assertTrue(result.items().getFirst().matchReasons().stream().anyMatch(reason -> reason.contains("术语表")));
        assertEquals(QueryTokenResolutionStatus.RESOLVED,
                result.summary().queryTokens().stream()
                        .filter(token -> "费用".equals(token.normalizedToken()))
                        .findFirst()
                        .orElseThrow()
                        .resolutionStatus());
        assertTrue(result.items().getFirst().evidence().stream().anyMatch(trace ->
                "BUSINESS_GLOSSARY".equals(trace.sourceType())
                        && "GLOSSARY_LONGEST_MATCH".equals(trace.ruleCode())));
    }

    @Test
    void search_tokenizesAcronymCamelAndNumberNameOnce() {
        FieldRepository repository = mock(FieldRepository.class);
        BusinessGlossaryService glossaryService = mock(BusinessGlossaryService.class);
        when(glossaryService.match(1L, "HTTPStatus2Code")).thenReturn(List.of());
        Field field = field("http_status_2_code", "HTTP 状态码", "int", "HTTP 状态码", "", "enabled");
        field.setId(21L);
        when(repository.findAllByProjectId(1L)).thenReturn(List.of(field));
        FieldServiceImpl service = service(
                repository,
                mock(FieldSourceRepository.class),
                mock(StandardChangeLogService.class),
                glossaryService);

        FieldSearchResult result = service.search(new FieldSearchReq(
                1L, "HTTPStatus2Code", null, null, null, null, null, 10));

        assertEquals("http_status_2_code", result.items().getFirst().field().getName());
        assertEquals(List.of("http", "status", "2", "code"), result.summary().queryTokens().stream()
                .map(token -> token.normalizedToken())
                .toList());
        assertTrue(result.items().getFirst().evidence().stream().anyMatch(trace ->
                "QUERY_TOKEN".equals(trace.sourceType()) && "NAME_SPLIT".equals(trace.ruleCode())));
        verify(glossaryService, times(1)).match(1L, "HTTPStatus2Code");
    }

    @Test
    void search_doesNotTreatLongSharedPrefixAsAFieldTokenMatch() {
        String sharedPrefix = "a".repeat(64);
        String query = sharedPrefix + "x";
        FieldRepository repository = mock(FieldRepository.class);
        Field field = field(sharedPrefix + "y", "长字段", "varchar(100)", null, "", "enabled");
        field.setId(25L);
        when(repository.findAllByProjectId(1L)).thenReturn(List.of(field));
        FieldServiceImpl service = service(repository, mock(StandardChangeLogService.class));

        FieldSearchResult result = service.search(new FieldSearchReq(
                1L, query, null, null, null, null, null, 10));

        assertTrue(result.items().isEmpty());
    }

    @Test
    void search_scoresOnlyTheSecretSafeNormalizedQuery() {
        FieldRepository repository = mock(FieldRepository.class);
        BusinessGlossaryService glossaryService = mock(BusinessGlossaryService.class);
        Field rawSecretName = field("very_secret_value", "敏感原值", "varchar(100)", null, "", "enabled");
        rawSecretName.setId(27L);
        when(repository.findAllByProjectId(1L)).thenReturn(List.of(rawSecretName));
        when(glossaryService.match(1L, "password=[REDACTED]")).thenReturn(List.of());
        FieldServiceImpl service = service(
                repository,
                mock(FieldSourceRepository.class),
                mock(StandardChangeLogService.class),
                glossaryService);

        FieldSearchResult result = service.search(new FieldSearchReq(
                1L, "password=very-secret-value", null, null, null, null, null, 10));

        assertTrue(result.items().isEmpty());
        assertFalse(result.query().contains("very-secret-value"));
        verify(glossaryService).match(1L, "password=[REDACTED]");
    }

    @Test
    void suggest_keepsGlossaryTraceForLongAbbreviationEvidence() {
        String abbreviation = "a".repeat(70);
        FieldRepository repository = mock(FieldRepository.class);
        BusinessGlossaryService glossaryService = mock(BusinessGlossaryService.class);
        Field amount = field("order_amount", "订单金额", "bigint", null, "", "enabled");
        amount.setId(26L);
        when(repository.findAllByProjectId(1L)).thenReturn(List.of(amount));
        when(glossaryService.match(1L, abbreviation)).thenReturn(List.of(
                new GlossaryMatch(34L, "订单金额", abbreviation, "ABBREVIATION", 104, 26L,
                        "order_amount", Set.of(), false, "术语表：长缩写 -> order_amount")));
        FieldServiceImpl service = service(
                repository,
                mock(FieldSourceRepository.class),
                mock(StandardChangeLogService.class),
                glossaryService);

        FieldSuggestion suggestion = service.suggest(1L, abbreviation, 5).getFirst();

        assertTrue(suggestion.evidence().stream().anyMatch(trace ->
                Long.valueOf(34L).equals(trace.sourceId())
                        && "ABBREVIATION_EXPANSION".equals(trace.ruleCode())));
    }

    @Test
    void suggest_distinguishesLongTermAndAbbreviationFromTheSameGlossaryEntry() {
        String sharedPrefix = "a".repeat(61);
        String longTerm = sharedPrefix + "term";
        String longAbbreviation = sharedPrefix + "abbr";
        String query = longTerm + "_" + longAbbreviation;
        FieldRepository repository = mock(FieldRepository.class);
        BusinessGlossaryService glossaryService = mock(BusinessGlossaryService.class);
        Field amount = field("order_amount", "订单金额", "bigint", null, "", "enabled");
        amount.setId(28L);
        when(repository.findAllByProjectId(1L)).thenReturn(List.of(amount));
        when(glossaryService.match(1L, query)).thenReturn(List.of(
                new GlossaryMatch(35L, longTerm, longTerm, "TERM", 122, 28L,
                        "order_amount", Set.of(), false, "术语表：长术语 -> order_amount"),
                new GlossaryMatch(35L, longTerm, longAbbreviation, "ABBREVIATION", 104, 28L,
                        "order_amount", Set.of(), false, "术语表：长缩写 -> order_amount")));
        FieldServiceImpl service = service(
                repository,
                mock(FieldSourceRepository.class),
                mock(StandardChangeLogService.class),
                glossaryService);

        FieldSuggestion suggestion = service.suggest(1L, query, 5).getFirst();

        assertTrue(suggestion.evidence().stream().anyMatch(trace ->
                Long.valueOf(35L).equals(trace.sourceId())
                        && "GLOSSARY_LONGEST_MATCH".equals(trace.ruleCode())));
        assertTrue(suggestion.evidence().stream().anyMatch(trace ->
                Long.valueOf(35L).equals(trace.sourceId())
                        && "ABBREVIATION_EXPANSION".equals(trace.ruleCode())));
    }

    @Test
    void suggest_keepsDirectFieldNameAheadOfExpandedAbbreviation() {
        FieldRepository repository = mock(FieldRepository.class);
        BusinessGlossaryService glossaryService = mock(BusinessGlossaryService.class);
        Field direct = field("amt", "AMT", "varchar(20)", null, "", "enabled");
        direct.setId(22L);
        Field expanded = field("order_amount", "订单金额", "bigint", null, "", "enabled");
        expanded.setId(23L);
        when(repository.findAllByProjectId(1L)).thenReturn(List.of(expanded, direct));
        when(glossaryService.match(1L, "amt")).thenReturn(List.of(
                new GlossaryMatch(9L, "订单金额", "amt", "ABBREVIATION", 104, 23L,
                        "order_amount", Set.of("order_amount"), false, "术语表：amt -> order_amount")));
        FieldServiceImpl service = service(
                repository,
                mock(FieldSourceRepository.class),
                mock(StandardChangeLogService.class),
                glossaryService);

        List<FieldSuggestion> suggestions = service.suggest(1L, "amt", 5);

        assertEquals("amt", suggestions.getFirst().recommendedName());
        assertTrue(suggestions.getFirst().score() > suggestions.get(1).score());
    }

    @Test
    void suggest_correlatesEachGlossaryTraceWithItsQueryTokenSource() {
        FieldRepository repository = mock(FieldRepository.class);
        BusinessGlossaryService glossaryService = mock(BusinessGlossaryService.class);
        Field amount = field("order_amount", "订单金额", "bigint", null, "", "enabled");
        amount.setId(23L);
        when(repository.findAllByProjectId(1L)).thenReturn(List.of(amount));
        when(glossaryService.match(1L, "order_amt")).thenReturn(List.of(
                new GlossaryMatch(31L, "订单", "order", "TERM", 122, 23L,
                        "order_amount", Set.of(), false, "术语表：order -> order_amount"),
                new GlossaryMatch(32L, "订单金额", "amt", "ABBREVIATION", 104, 23L,
                        "order_amount", Set.of(), false, "术语表：amt -> order_amount")));
        FieldServiceImpl service = service(
                repository,
                mock(FieldSourceRepository.class),
                mock(StandardChangeLogService.class),
                glossaryService);

        FieldSuggestion suggestion = service.suggest(1L, "order_amt", 5).getFirst();

        assertTrue(suggestion.evidence().stream().anyMatch(trace ->
                Long.valueOf(31L).equals(trace.sourceId())
                        && "GLOSSARY_LONGEST_MATCH".equals(trace.ruleCode())));
        assertTrue(suggestion.evidence().stream().anyMatch(trace ->
                Long.valueOf(32L).equals(trace.sourceId())
                        && "ABBREVIATION_EXPANSION".equals(trace.ruleCode())));
    }

    @Test
    void search_explainsGlossaryMatchThatAppliesThroughExampleFieldOnly() {
        FieldRepository repository = mock(FieldRepository.class);
        BusinessGlossaryService glossaryService = mock(BusinessGlossaryService.class);
        Field amount = field("payment_amount", "支付金额", "bigint", null, "", "enabled");
        amount.setId(24L);
        when(repository.findAllByProjectId(1L)).thenReturn(List.of(amount));
        when(glossaryService.match(1L, "fee")).thenReturn(List.of(
                new GlossaryMatch(33L, "费用", "fee", "SYNONYM", 116, null,
                        null, Set.of("payment_amount"), false, "术语表：fee -> payment_amount")));
        FieldServiceImpl service = service(
                repository,
                mock(FieldSourceRepository.class),
                mock(StandardChangeLogService.class),
                glossaryService);

        FieldSearchItem item = service.search(new FieldSearchReq(
                1L, "fee", null, null, null, null, null, 10)).items().getFirst();

        assertTrue(item.evidence().stream().anyMatch(trace ->
                Long.valueOf(33L).equals(trace.sourceId())
                        && "GLOSSARY_LONGEST_MATCH".equals(trace.ruleCode())));
    }

    @Test
    void suggest_ambiguousAbbreviationRequestsConfirmationWithoutChoosingCanonical() {
        FieldRepository repository = mock(FieldRepository.class);
        BusinessGlossaryService glossaryService = mock(BusinessGlossaryService.class);
        when(repository.findAllByProjectId(1L)).thenReturn(List.of());
        when(glossaryService.match(1L, "amt")).thenReturn(List.of(new GlossaryMatch(
                10L,
                null,
                "amt",
                "ABBREVIATION",
                0,
                null,
                null,
                Set.of(),
                false,
                "同一缩写指向多个 canonical 字段",
                QueryTokenResolutionStatus.AMBIGUOUS,
                List.of(10L, 11L))));
        FieldServiceImpl service = service(
                repository,
                mock(FieldSourceRepository.class),
                mock(StandardChangeLogService.class),
                glossaryService);

        FieldSuggestion fallback = service.suggest(1L, "amt", 5).getFirst();

        assertFalse(fallback.existing());
        assertTrue(fallback.matchReason().contains("歧义") || fallback.matchReason().contains("人工确认"));
        assertEquals(QueryTokenResolutionStatus.AMBIGUOUS,
                fallback.queryTokens().getFirst().resolutionStatus());
    }

    @Test
    void suggest_directFieldKeepsPriorityAndExplainsAmbiguousAbbreviation() {
        FieldRepository repository = mock(FieldRepository.class);
        BusinessGlossaryService glossaryService = mock(BusinessGlossaryService.class);
        Field direct = field("amt", "AMT", "varchar(20)", null, "", "enabled");
        direct.setId(22L);
        when(repository.findAllByProjectId(1L)).thenReturn(List.of(direct));
        when(glossaryService.match(1L, "amt")).thenReturn(List.of(new GlossaryMatch(
                10L,
                null,
                "amt",
                "ABBREVIATION",
                0,
                null,
                null,
                Set.of(),
                false,
                "同一缩写指向多个 canonical 字段",
                QueryTokenResolutionStatus.AMBIGUOUS,
                List.of(10L, 11L))));
        FieldServiceImpl service = service(
                repository,
                mock(FieldSourceRepository.class),
                mock(StandardChangeLogService.class),
                glossaryService);

        FieldSuggestion suggestion = service.suggest(1L, "amt", 5).getFirst();

        assertTrue(suggestion.existing());
        assertEquals("amt", suggestion.recommendedName());
        assertTrue(suggestion.evidence().stream().anyMatch(trace ->
                "ABBREVIATION_AMBIGUOUS".equals(trace.ruleCode()) && Integer.valueOf(0).equals(trace.confidence())));
    }

    @Test
    void suggest_matchesDeterministicGoldenRecommendationFixture() throws Exception {
        JsonNode fixture = objectMapper.readTree(readResource(
                "fixtures/querynormalization/deterministic-name-tokenization.json"));

        assertEquals(1, fixture.path("schemaVersion").asInt());
        for (JsonNode scenario : fixture.path("recommendationCases")) {
            FieldRepository repository = mock(FieldRepository.class);
            BusinessGlossaryService glossaryService = mock(BusinessGlossaryService.class);
            List<Field> fields = fieldsFromFixture(scenario.path("fields"));
            String query = scenario.path("query").asText();
            when(repository.findAllByProjectId(1L)).thenReturn(fields);
            when(glossaryService.match(1L, query)).thenReturn(glossaryMatchesFromFixture(
                    scenario.path("glossaryMatches")));
            FieldServiceImpl service = service(
                    repository,
                    mock(FieldSourceRepository.class),
                    mock(StandardChangeLogService.class),
                    glossaryService);

            List<FieldSuggestion> suggestions = service.suggest(1L, query, scenario.path("limit").asInt());
            List<String> expectedOrder = stringValues(scenario.path("expectedOrder"));
            String scenarioId = scenario.path("id").asText();

            assertTrue(suggestions.size() >= expectedOrder.size(), scenarioId);
            assertEquals(
                    expectedOrder,
                    suggestions.stream()
                            .limit(expectedOrder.size())
                            .map(FieldSuggestion::recommendedName)
                            .toList(),
                    scenarioId);
            assertEquals(
                    stringValues(scenario.path("expectedResolutionStatuses")),
                    suggestions.getFirst().queryTokens().stream()
                            .map(token -> token.resolutionStatus().name())
                            .toList(),
                    scenarioId);
            assertEquals(scenario.path("expectedTopExisting").asBoolean(), suggestions.getFirst().existing(), scenarioId);
            if (scenario.path("strictTopScore").asBoolean()) {
                assertTrue(suggestions.getFirst().score() > suggestions.get(1).score(), scenarioId);
            }
        }
    }

    @Test
    void search_filtersByCategoryTagStatusSensitiveAndSourceBatch() {
        FieldRepository repository = mock(FieldRepository.class);
        FieldSourceRepository sourceRepository = mock(FieldSourceRepository.class);
        Field mobile = field("mobile_no", "手机号", "varchar(20)", "用户手机号", "phone", "enabled");
        mobile.setId(1L);
        mobile.setCategory("contact");
        mobile.setTags("pii");
        mobile.setSensitive(true);
        Field amount = field("amount_cent", "金额（分）", "bigint", "支付金额", "amount", "deprecated");
        amount.setId(3L);
        amount.setCategory("money");
        amount.setTags("finance");
        amount.setSensitive(false);
        when(repository.findAllByProjectId(1L)).thenReturn(List.of(mobile, amount));
        when(sourceRepository.findFieldIdsByProjectAndBatch(1L, 77L)).thenReturn(List.of(3L));
        FieldServiceImpl service = service(repository, sourceRepository, mock(StandardChangeLogService.class));

        FieldSearchResult result = service.search(new FieldSearchReq(
                1L, null, "money", "finance", "deprecated", false, 77L, 10));

        assertEquals(1, result.items().size());
        assertEquals("amount_cent", result.items().getFirst().field().getName());
        assertTrue(result.items().getFirst().matchReasons().contains("导入批次过滤命中: 77"));
        assertEquals(77L, result.summary().appliedFilters().get("sourceBatchId"));
        assertEquals(1, result.summary().matchedCount());
    }

    @Test
    void search_legacyParametersExposeDslExplanationWithoutChangingResults() {
        FieldRepository repository = mock(FieldRepository.class);
        Field amount = field("amount_cent", "金额（分）", "bigint", "支付金额", "amount", "enabled");
        amount.setId(3L);
        amount.setCategory("money");
        amount.setTags("finance");
        amount.setSensitive(false);
        when(repository.findAllByProjectId(1L)).thenReturn(List.of(amount));
        FieldServiceImpl service = service(repository, mock(StandardChangeLogService.class));

        FieldSearchResult result = service.search(new FieldSearchReq(
                1L, "金额", "money", "finance", "enabled", false, null, 10));

        assertEquals("amount_cent", result.items().getFirst().field().getName());
        assertEquals("FIELD", result.summary().querySummary().target());
        assertEquals("金额", result.summary().querySummary().text());
        assertEquals(1, result.summary().querySummary().resultCount());
        assertEquals(1, result.summary().querySummary().returnedCount());
        assertFalse(result.summary().querySummary().truncated());
        assertTrue(result.summary().dslAppliedFilters().stream().anyMatch(filter ->
                "category".equals(filter.field()) && "money".equals(filter.redactedValue())));
        assertTrue(result.summary().dslIgnoredFilters().isEmpty());
        assertTrue(result.summary().nextQueryHints().isEmpty());
    }

    @Test
    void search_requiresQueryOrFilter() {
        FieldRepository repository = mock(FieldRepository.class);
        FieldServiceImpl service = service(repository, mock(StandardChangeLogService.class));

        BizException ex = assertThrows(BizException.class, () -> service.search(new FieldSearchReq(
                1L, "!!!", null, null, null, null, null, 10)));

        assertTrue(ex.getMessage().contains("字段检索需要"));
        verify(repository, never()).findAllByProjectId(anyLong());
    }

    @Test
    void search_returnsNextActionWhenNoFieldMatches() {
        FieldRepository repository = mock(FieldRepository.class);
        when(repository.findAllByProjectId(1L)).thenReturn(List.of(
                field("mobile_no", "手机号", "varchar(20)", "用户手机号", "phone", "enabled")
        ));
        FieldServiceImpl service = service(repository, mock(StandardChangeLogService.class));

        FieldSearchResult result = service.search(new FieldSearchReq(
                1L, "发票抬头", null, null, null, null, null, 10));

        assertTrue(result.items().isEmpty());
        assertEquals(0, result.summary().matchedCount());
        assertFalse(result.summary().hints().isEmpty());
        assertTrue(result.nextActions().getFirst().contains("标准候选"));
    }

    @Test
    void suggestAiContract_exposesStableRecommendationFields() {
        FieldRepository repository = mock(FieldRepository.class);
        Field mobile = field("mobile_no", "手机号", "varchar(20)", "用户手机号", "phone,mobile,tel", "enabled");
        mobile.setId(15L);
        mobile.setSensitive(true);
        mobile.setCategory("contact");
        mobile.setCodeSetId(10L);
        mobile.setExampleValue("13800138000");
        when(repository.findAllByProjectId(1L)).thenReturn(List.of(mobile));
        FieldServiceImpl service = service(repository, mock(StandardChangeLogService.class));

        FieldSuggestion suggestion = service.suggest(1L, "用户手机号", 5).getFirst();

        JsonNode root = new ObjectMapper().valueToTree(suggestion);
        assertEquals("mobile_no", root.path("recommendedName").asText());
        assertTrue(root.path("existing").asBoolean());
        assertTrue(root.path("score").asInt() > 0);
        assertFalse(root.path("matchReason").asText().isBlank());
        JsonNode fieldNode = root.path("field");
        assertEquals("mobile_no", fieldNode.path("name").asText());
        assertEquals("手机号", fieldNode.path("displayName").asText());
        assertEquals("varchar(20)", fieldNode.path("dataType").asText());
        assertEquals("phone,mobile,tel", fieldNode.path("aliases").asText());
        assertTrue(fieldNode.path("sensitive").asBoolean());
        assertEquals("enabled", fieldNode.path("status").asText());
        assertEquals("contact", fieldNode.path("category").asText());
        assertEquals(10L, fieldNode.path("codeSetId").asLong());
        assertEquals("13800138000", fieldNode.path("exampleValue").asText());

        JsonNode evidence = root.path("evidence");
        assertTrue(evidence.isArray());
        assertFalse(evidence.isEmpty());
        assertEquals("FIELD", evidence.get(0).path("sourceType").asText());
        assertEquals(15L, evidence.get(0).path("sourceId").asLong());
        assertFalse(evidence.get(0).path("matchReason").asText().isBlank());
        assertTrue(evidence.get(0).path("confidence").asInt() > 0);
        assertFalse(evidence.get(0).path("docsRef").asText().isBlank());
    }

    @Test
    void suggest_matchesSemanticSynonymAndPinyinAbbreviation() {
        FieldRepository repository = mock(FieldRepository.class);
        when(repository.findAllByProjectId(1L)).thenReturn(List.of(
                field("mobile_no", "联系电话", "varchar(20)", "用户联系号码", "", "enabled"),
                field("user_name", "用户姓名", "varchar(64)", "用户姓名", "", "enabled")
        ));
        FieldServiceImpl service = service(repository, mock(StandardChangeLogService.class));

        List<FieldSuggestion> suggestions = service.suggest(1L, "用户sjh", 5);

        assertEquals("mobile_no", suggestions.getFirst().recommendedName());
        assertTrue(suggestions.getFirst().matchReason().contains("语义词"));
    }

    @Test
    void suggest_penalizesGenericOnlyMatches() {
        FieldRepository repository = mock(FieldRepository.class);
        when(repository.findAllByProjectId(1L)).thenReturn(List.of(
                field("user_profile", "用户资料", "jsonb", "用户扩展信息", "", "enabled"),
                field("mobile_no", "联系电话", "varchar(20)", "联系号码", "", "enabled")
        ));
        FieldServiceImpl service = service(repository, mock(StandardChangeLogService.class));

        List<FieldSuggestion> suggestions = service.suggest(1L, "用户手机号", 5);

        assertEquals("mobile_no", suggestions.getFirst().recommendedName());
        assertTrue(suggestions.getFirst().score() > suggestions.get(1).score());
    }

    @Test
    void suggest_marksSensitiveFieldInReason() {
        FieldRepository repository = mock(FieldRepository.class);
        Field idCard = field("id_card_no", "身份证号", "varchar(32)", "证件号码", "", "enabled");
        idCard.setSensitive(true);
        when(repository.findAllByProjectId(1L)).thenReturn(List.of(idCard));
        FieldServiceImpl service = service(repository, mock(StandardChangeLogService.class));

        List<FieldSuggestion> suggestions = service.suggest(1L, "sfzh", 5);

        assertEquals("id_card_no", suggestions.getFirst().recommendedName());
        assertTrue(suggestions.getFirst().matchReason().contains("敏感"));
    }

    @Test
    void suggest_generatesCanonicalFallbackNamesForKnownSemanticGroups() {
        FieldRepository repository = mock(FieldRepository.class);
        when(repository.findAllByProjectId(1L)).thenReturn(List.of());
        FieldServiceImpl service = service(repository, mock(StandardChangeLogService.class));

        assertEquals("user_id", service.suggest(1L, "用户编号 uid", 5).getFirst().recommendedName());
        assertEquals("mobile_no", service.suggest(1L, "客户手机", 5).getFirst().recommendedName());
        assertEquals("amount_cent", service.suggest(1L, "付款金额", 5).getFirst().recommendedName());
    }

    @Test
    void suggest_doesNotMatchShortEnglishKeywordInsideUnrelatedToken() {
        FieldRepository repository = mock(FieldRepository.class);
        when(repository.findAllByProjectId(1L)).thenReturn(List.of(
                field("updated_at", "更新时间", "timestamp", "更新时间", "", "enabled")
        ));
        FieldServiceImpl service = service(repository, mock(StandardChangeLogService.class));

        FieldSuggestion suggestion = service.suggest(1L, "status", 5).getFirst();

        assertFalse(suggestion.existing());
        assertEquals("status", suggestion.recommendedName());
    }

    @Test
    void suggest_filtersDisabledFields() {
        FieldRepository repository = mock(FieldRepository.class);
        when(repository.findAllByProjectId(1L)).thenReturn(List.of(
                field("mobile_no", "手机号", "varchar(20)", "用户手机号", "phone,mobile,tel", "disabled")
        ));
        FieldServiceImpl service = service(repository, mock(StandardChangeLogService.class));

        List<FieldSuggestion> suggestions = service.suggest(1L, "手机号", 5);

        assertFalse(suggestions.isEmpty());
        assertFalse(suggestions.getFirst().existing());
        assertNull(suggestions.getFirst().field());
    }

    @Test
    void suggest_doesNotDirectlyAdoptFieldWhenUsageContractAvoidsScenario() {
        FieldRepository repository = mock(FieldRepository.class);
        Field displayMobile = field("display_mobile", "展示手机号", "varchar(20)", "脱敏展示手机号", "mobile", "enabled");
        displayMobile.setId(30L);
        displayMobile.setAvoidWhen("写入数据库或作为 join key");
        when(repository.findAllByProjectId(1L)).thenReturn(List.of(displayMobile));
        FieldServiceImpl service = service(repository, mock(StandardChangeLogService.class));

        FieldSuggestion suggestion = service.suggest(1L, "手机号写入数据库字段", 5).getFirst();

        assertFalse(suggestion.existing());
        assertNull(suggestion.field());
        assertTrue(suggestion.matchReason().contains("字段使用契约"));
        assertTrue(suggestion.evidence().stream().anyMatch(evidence -> evidence.sourceId().equals(displayMobile.getId())));
    }

    @Test
    void suggest_doesNotDowngradeWhenAvoidScenarioOnlySharesOneChineseBigram() {
        FieldRepository repository = mock(FieldRepository.class);
        Field amount = field("amount_cent", "订单金额", "bigint", "订单金额以分存储", "amount", "enabled");
        amount.setId(20L);
        amount.setAvoidWhen("展示金额时不要直接输出分单位");
        when(repository.findAllByProjectId(1L)).thenReturn(List.of(amount));
        FieldServiceImpl service = service(repository, mock(StandardChangeLogService.class));

        FieldSuggestion suggestion = service.suggest(1L, "统计订单金额", 5).getFirst();

        assertTrue(suggestion.existing());
        assertEquals("amount_cent", suggestion.recommendedName());
        assertSame(amount, suggestion.field());
    }

    @Test
    void suggest_skipsNonEnabledFieldsByDefault() {
        FieldRepository repository = mock(FieldRepository.class);
        Field draft = field("draft_mobile_no", "草稿手机号", "varchar(20)", "用户手机号", "phone,mobile", "draft");
        Field deprecated = field("old_mobile_no", "旧手机号", "varchar(20)", "用户手机号", "phone,mobile", "deprecated");
        Field disabled = field("disabled_mobile_no", "停用手机号", "varchar(20)", "用户手机号", "phone,mobile", "disabled");
        deprecated.setReplacementReason("历史字段，改用 mobile_no");
        when(repository.findAllByProjectId(1L)).thenReturn(List.of(draft, deprecated, disabled));
        FieldServiceImpl service = service(repository, mock(StandardChangeLogService.class));

        FieldSuggestion suggestion = service.suggest(1L, "手机号", 5).getFirst();

        assertFalse(suggestion.existing());
        assertNull(suggestion.field());
        assertEquals("mobile_no", suggestion.recommendedName());
    }

    @Test
    void search_explicitNonEnabledFieldShowsLifecycleGuidance() {
        FieldRepository repository = mock(FieldRepository.class);
        Field legacy = field("old_mobile_no", "旧手机号", "varchar(20)", "历史手机号", "phone,mobile", "deprecated");
        legacy.setId(1L);
        legacy.setReplacementFieldId(2L);
        legacy.setReplacementReason("历史兼容字段，改用 mobile_no");
        when(repository.findAllByProjectId(1L)).thenReturn(List.of(legacy));
        FieldServiceImpl service = service(repository, mock(StandardChangeLogService.class));

        FieldSearchResult result = service.search(new FieldSearchReq(
                1L, "旧手机号", null, null, "deprecated", null, null, 10));

        assertEquals(1, result.items().size());
        assertTrue(result.items().getFirst().recommendedUse().contains("改用 mobile_no"));
        assertTrue(result.items().getFirst().nextActions().stream()
                .anyMatch(action -> action.contains("replacementFieldId=2")));
    }

    @Test
    void search_aliasResultIncludesStableReferenceMetadata() {
        FieldRepository repository = mock(FieldRepository.class);
        Field current = field("mobile_no", "手机号", "varchar(20)", "用户手机号", "phone,mobile_phone", "enabled");
        current.setId(10L);
        Field replacement = field("user_mobile_no", "用户手机号", "varchar(20)", "用户手机号", "", "enabled");
        replacement.setId(12L);
        Field legacy = field("old_mobile_no", "旧手机号", "varchar(20)", "历史手机号", "legacy_phone", "deprecated");
        legacy.setId(11L);
        legacy.setReplacementFieldId(12L);
        when(repository.findAllByProjectId(1L)).thenReturn(List.of(current, legacy));
        when(repository.findById(12L)).thenReturn(Optional.of(replacement));
        FieldServiceImpl service = service(repository, mock(StandardChangeLogService.class));

        FieldSearchResult aliasResult = service.search(new FieldSearchReq(
                1L, "mobile_phone", null, null, null, null, null, 10));
        FieldSearchItem aliasItem = aliasResult.items().getFirst();
        assertEquals("field:1:10", aliasItem.stableRef());
        assertEquals("field:1:10", aliasItem.canonicalRef());
        assertEquals("enabled", aliasItem.lifecycleStatus());
        assertEquals("mobile_phone", aliasItem.matchedAlias());

        FieldSearchResult legacyResult = service.search(new FieldSearchReq(
                1L, "legacy_phone", null, null, "deprecated", null, null, 10));
        FieldSearchItem legacyItem = legacyResult.items().getFirst();
        assertEquals("field:1:11", legacyItem.stableRef());
        assertEquals("field:1:12", legacyItem.canonicalRef());
        assertEquals("deprecated", legacyItem.lifecycleStatus());
        assertEquals("legacy_phone", legacyItem.matchedAlias());
    }

    @Test
    void search_redactsSecretLikeMatchedAlias() {
        FieldRepository repository = mock(FieldRepository.class);
        Field current = field("debug_token", "调试令牌", "varchar(200)", "调试字段", "token=raw-secret-123", "enabled");
        current.setId(10L);
        when(repository.findAllByProjectId(1L)).thenReturn(List.of(current));
        FieldServiceImpl service = service(repository, mock(StandardChangeLogService.class));

        FieldSearchResult result = service.search(new FieldSearchReq(
                1L, "token", null, null, null, null, null, 10));

        FieldSearchItem item = result.items().getFirst();
        assertEquals("token=[REDACTED]", item.matchedAlias());
        assertFalse(item.matchedAlias().contains("raw-secret-123"));
    }

    @Test
    void searchAndSuggestRecallCurrentFieldFromHistoryWithoutOutrankingCurrentAlias() {
        FieldRepository repository = mock(FieldRepository.class);
        FieldHistoricalAliasService historyService = mock(FieldHistoricalAliasService.class);
        Field historical = field("mobile_no", "手机号", "varchar(20)", "当前手机号", null, "enabled");
        historical.setId(10L);
        historical.setProjectId(1L);
        Field currentAlias = field("former_phone_field", "现行别名字段", "varchar(20)", null, "former_phone", "enabled");
        currentAlias.setId(11L);
        currentAlias.setProjectId(1L);
        when(repository.findAllByProjectId(1L)).thenReturn(List.of(historical, currentAlias));
        when(historyService.load(1L, List.of(historical, currentAlias))).thenReturn(Map.of(
                10L, List.of(
                        new FieldHistoricalAlias(10L, "legacy_phone", 100L),
                        new FieldHistoricalAlias(10L, "former_phone", 101L))));
        BusinessGlossaryService glossaryService = mock(BusinessGlossaryService.class);
        when(glossaryService.match(anyLong(), anyString())).thenReturn(List.of());
        FieldServiceImpl service = service(
                repository,
                mock(FieldSourceRepository.class),
                mock(StandardChangeLogService.class),
                glossaryService,
                historyService);

        FieldSearchItem historicalItem = service.search(new FieldSearchReq(
                1L, "legacy_phone", null, null, null, null, null, 10)).items().getFirst();
        assertEquals("mobile_no", historicalItem.field().getName());
        assertEquals("legacy_phone", historicalItem.matchedAlias());
        assertTrue(historicalItem.matchReasons().stream().anyMatch(reason -> reason.contains("历史名称")));
        assertTrue(historicalItem.evidence().stream().anyMatch(trace ->
                "FIELD_CHANGE_LOG".equals(trace.sourceType())
                        && Long.valueOf(100L).equals(trace.sourceId())
                        && "dataspec://change-logs/100".equals(trace.docsRef())));

        List<FieldSuggestion> suggestions = service.suggest(1L, "legacy_phone", 5);
        assertEquals("mobile_no", suggestions.getFirst().recommendedName());
        assertTrue(suggestions.getFirst().matchReason().contains("历史名称"));

        FieldSearchResult ranking = service.search(new FieldSearchReq(
                1L, "former_phone", null, null, null, null, null, 10));
        assertEquals("former_phone_field", ranking.items().getFirst().field().getName());
        assertTrue(ranking.items().getFirst().score() > ranking.items().get(1).score());
    }

    @Test
    void suggest_returnsFallbackSnakeCaseNameWhenNoExistingFieldMatches() {
        FieldRepository repository = mock(FieldRepository.class);
        when(repository.findAllByProjectId(1L)).thenReturn(List.of());
        FieldServiceImpl service = service(repository, mock(StandardChangeLogService.class));

        List<FieldSuggestion> suggestions = service.suggest(1L, "客户生日", 5);

        assertEquals(1, suggestions.size());
        FieldSuggestion fallback = suggestions.getFirst();
        assertFalse(fallback.existing());
        assertEquals("customer_birthday", fallback.recommendedName());
        assertEquals(0, fallback.score());
        assertNull(fallback.field());
    }

    @Test
    void suggest_rejectsDescriptionWithoutSearchableContent() {
        FieldRepository repository = mock(FieldRepository.class);
        FieldServiceImpl service = service(repository, mock(StandardChangeLogService.class));

        assertThrows(BizException.class, () -> service.suggest(1L, "!!!", 5));
        verify(repository, never()).findAllByProjectId(anyLong());
    }

    @Test
    void getById_rejectsUnauthorizedProject() {
        FieldRepository repository = mock(FieldRepository.class);
        Field field = new Field();
        field.setId(9L);
        field.setProjectId(1L);
        field.setName("mobile_no");
        when(repository.findById(9L)).thenReturn(Optional.of(field));
        DataSpecSecurityContext.set(new ApiTokenPrincipal("limited", "bob", false, Set.of(2L)));
        FieldServiceImpl service = service(repository, mock(StandardChangeLogService.class));

        BizException ex = assertThrows(BizException.class, () -> service.getById(9L));

        assertEquals(403, ex.getCode());
    }

    private Field field(String name, String displayName, String dataType, String comment,
                        String aliases, String status) {
        Field field = new Field();
        field.setProjectId(1L);
        field.setName(name);
        field.setDisplayName(displayName);
        field.setDataType(dataType);
        field.setComment(comment);
        field.setAliases(aliases);
        field.setStatus(status);
        return field;
    }

    private List<Field> fieldsFromFixture(JsonNode values) {
        List<Field> fields = new ArrayList<>();
        for (JsonNode value : values) {
            Field field = field(
                    value.path("name").asText(),
                    nullableText(value.path("displayName")),
                    value.path("dataType").asText(),
                    nullableText(value.path("comment")),
                    nullableText(value.path("aliases")),
                    value.path("status").asText("enabled"));
            field.setId(value.path("id").asLong());
            fields.add(field);
        }
        return List.copyOf(fields);
    }

    private List<GlossaryMatch> glossaryMatchesFromFixture(JsonNode values) {
        List<GlossaryMatch> matches = new ArrayList<>();
        for (JsonNode value : values) {
            matches.add(new GlossaryMatch(
                    nullableLong(value.path("glossaryId")),
                    nullableText(value.path("term")),
                    value.path("matchedToken").asText(),
                    value.path("matchType").asText(),
                    value.path("score").asInt(),
                    nullableLong(value.path("canonicalFieldId")),
                    nullableText(value.path("canonicalFieldName")),
                    Set.copyOf(stringValues(value.path("exampleFields"))),
                    value.path("disabledTerm").asBoolean(),
                    value.path("reason").asText(),
                    QueryTokenResolutionStatus.valueOf(value.path("resolutionStatus").asText()),
                    longValues(value.path("glossaryIds"))));
        }
        return List.copyOf(matches);
    }

    private List<String> stringValues(JsonNode values) {
        return java.util.stream.StreamSupport.stream(values.spliterator(), false)
                .map(JsonNode::asText)
                .toList();
    }

    private List<Long> longValues(JsonNode values) {
        return java.util.stream.StreamSupport.stream(values.spliterator(), false)
                .map(JsonNode::asLong)
                .toList();
    }

    private String nullableText(JsonNode value) {
        return value == null || value.isMissingNode() || value.isNull() ? null : value.asText();
    }

    private Long nullableLong(JsonNode value) {
        return value == null || value.isMissingNode() || value.isNull() ? null : value.asLong();
    }

    private String readResource(String path) throws IOException {
        try (var input = getClass().getClassLoader().getResourceAsStream(path)) {
            assertNotNull(input, "测试资源不存在: " + path);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private StandardChangeLog fieldLog(Long id, Long projectId, Long fieldId, String action, String beforeJson) {
        StandardChangeLog log = new StandardChangeLog();
        log.setId(id);
        log.setProjectId(projectId);
        log.setTargetType(StandardChangeLogService.TARGET_FIELD);
        log.setTargetId(fieldId);
        log.setAction(action);
        log.setBeforeJson(beforeJson);
        return log;
    }

    private FieldServiceImpl service(FieldRepository repository, StandardChangeLogService changeLogService) {
        return service(repository, mock(FieldSourceRepository.class), changeLogService);
    }

    private FieldServiceImpl service(FieldRepository repository, FieldSourceRepository sourceRepository,
                                     StandardChangeLogService changeLogService) {
        BusinessGlossaryService glossaryService = mock(BusinessGlossaryService.class);
        when(glossaryService.match(anyLong(), anyString())).thenReturn(List.of());
        return service(repository, sourceRepository, changeLogService, glossaryService);
    }

    private FieldServiceImpl service(FieldRepository repository, FieldSourceRepository sourceRepository,
                                     StandardChangeLogService changeLogService,
                                     BusinessGlossaryService glossaryService) {
        FieldHistoricalAliasService historyService = mock(FieldHistoricalAliasService.class);
        when(historyService.load(anyLong(), anyList())).thenReturn(Map.of());
        return service(repository, sourceRepository, changeLogService, glossaryService, historyService);
    }

    private FieldServiceImpl service(FieldRepository repository, FieldSourceRepository sourceRepository,
                                     StandardChangeLogService changeLogService,
                                     BusinessGlossaryService glossaryService,
                                     FieldHistoricalAliasService historyService) {
        FieldSemanticRuleService semanticRuleService = mock(FieldSemanticRuleService.class);
        MetricDefinitionService metricDefinitionService = mock(MetricDefinitionService.class);
        when(semanticRuleService.list(anyLong(), nullable(Long.class), nullable(String.class), nullable(String.class)))
                .thenReturn(List.of());
        when(metricDefinitionService.list(anyLong(), nullable(String.class), nullable(String.class), nullable(Long.class)))
                .thenReturn(List.of());
        return new FieldServiceImpl(
                repository,
                sourceRepository,
                changeLogService,
                historyService,
                objectMapper,
                new QueryNormalizationServiceImpl(new NameLexicalTokenizer(), glossaryService),
                semanticRuleService,
                metricDefinitionService);
    }
}
