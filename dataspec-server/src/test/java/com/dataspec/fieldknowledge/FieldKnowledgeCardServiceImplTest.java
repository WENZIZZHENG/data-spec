package com.dataspec.fieldknowledge;

import com.dataspec.enumdict.entity.EnumDict;
import com.dataspec.enumdict.entity.EnumValue;
import com.dataspec.enumdict.repository.EnumDictRepository;
import com.dataspec.field.entity.Field;
import com.dataspec.field.repository.FieldRepository;
import com.dataspec.fieldknowledge.model.EnumValueHint;
import com.dataspec.fieldknowledge.model.FieldKnowledgeCardResp;
import com.dataspec.fieldknowledge.service.impl.FieldKnowledgeCardServiceImpl;
import com.dataspec.fieldsemantic.model.FieldSemanticRuleResp;
import com.dataspec.fieldsemantic.service.FieldSemanticRuleService;
import com.dataspec.metric.model.MetricDefinitionResp;
import com.dataspec.metric.service.MetricDefinitionService;
import com.dataspec.security.context.DataSpecSecurityContext;
import com.dataspec.standardusageexample.entity.StandardUsageExample;
import com.dataspec.standardusageexample.service.StandardUsageExampleService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FieldKnowledgeCardServiceImplTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @AfterEach
    void tearDown() {
        DataSpecSecurityContext.clear();
    }

    @Test
    void get_returnsValidCardForSparseField() {
        FieldRepository fieldRepository = mock(FieldRepository.class);
        EnumDictRepository enumDictRepository = mock(EnumDictRepository.class);
        FieldSemanticRuleService semanticRuleService = mock(FieldSemanticRuleService.class);
        MetricDefinitionService metricDefinitionService = mock(MetricDefinitionService.class);
        StandardUsageExampleService usageExampleService = mock(StandardUsageExampleService.class);
        Field field = field(10L, "external_order_no");
        field.setDataType("varchar(64)");
        field.setNullable(false);
        when(fieldRepository.findById(10L)).thenReturn(Optional.of(field));
        when(semanticRuleService.listRelatedToFields(1L, List.of(10L), 50)).thenReturn(List.of());
        when(metricDefinitionService.listRelatedToFields(1L, List.of(10L), 50)).thenReturn(List.of());
        when(usageExampleService.selectForAiContext(eq(1L), anyList(), isNull(), eq(50))).thenReturn(List.of());
        FieldKnowledgeCardServiceImpl service = new FieldKnowledgeCardServiceImpl(
                fieldRepository, enumDictRepository, semanticRuleService,
                metricDefinitionService, usageExampleService, objectMapper);

        FieldKnowledgeCardResp card = service.get(1L, 10L);

        assertThat(card.projectId()).isEqualTo(1L);
        assertThat(card.fieldId()).isEqualTo(10L);
        assertThat(card.stableRef()).isEqualTo("field:1:10");
        assertThat(card.semanticRules()).isEmpty();
        assertThat(card.enumHints()).isEmpty();
        assertThat(card.metricReferences()).isEmpty();
        assertThat(card.usageExamples()).isEmpty();
        assertThat(card.relatedFieldRefs()).isEmpty();
        assertThat(card.formatSummary()).contains("数据类型: varchar(64)", "可空: false");
    }

    @Test
    void get_aggregatesSemanticRulesEnumLifecycleMetricsAndEvidence() {
        FieldRepository fieldRepository = mock(FieldRepository.class);
        EnumDictRepository enumDictRepository = mock(EnumDictRepository.class);
        FieldSemanticRuleService semanticRuleService = mock(FieldSemanticRuleService.class);
        MetricDefinitionService metricDefinitionService = mock(MetricDefinitionService.class);
        StandardUsageExampleService usageExampleService = mock(StandardUsageExampleService.class);
        Field amount = field(10L, "amount_cent");
        amount.setDataType("bigint");
        amount.setCodeSetId(77L);
        amount.setSemanticSummary("金额以分存储");
        amount.setUpdatedAt(LocalDateTime.of(2026, 1, 1, 10, 0));
        when(fieldRepository.findById(10L)).thenReturn(Optional.of(amount));
        when(enumDictRepository.findDictById(77L)).thenReturn(Optional.of(enumDict(77L, 1L)));
        when(enumDictRepository.findValuesByEnumId(77L)).thenReturn(List.of(
                enumValue("PAID", "已支付", "enabled", null, 1),
                enumValue("LEGACY", "旧支付态", "deprecated", "PAID", 2)));
        FieldSemanticRuleResp semanticRule = new FieldSemanticRuleResp(
                5L,
                1L,
                10L,
                11L,
                "UNIT_CONVERSION",
                "cent -> yuan",
                "sum(amount_cent) / 100",
                "day",
                "以支付明细 amount_cent 为准",
                "统计订单金额",
                "不要把分当元展示",
                List.of("semantic-doc:amount"),
                "enabled",
                null,
                LocalDateTime.of(2026, 1, 2, 10, 0));
        when(semanticRuleService.listRelatedToFields(1L, List.of(10L), 50)).thenReturn(List.of(semanticRule));
        MetricDefinitionResp metric = new MetricDefinitionResp(
                9L,
                1L,
                "order_amount",
                "订单金额",
                "支付成功订单金额",
                List.of(10L),
                List.of(12L),
                "status = 'PAID'",
                "sum(amount_cent) / 100",
                "day",
                "财务口径",
                "SELECT sum(amount_cent) FROM orders",
                List.of("metric-doc:order"),
                "enabled",
                null,
                LocalDateTime.of(2026, 1, 3, 10, 0));
        when(metricDefinitionService.listRelatedToFields(1L, List.of(10L), 50)).thenReturn(List.of(metric));
        StandardUsageExample example = new StandardUsageExample();
        example.setId(100L);
        example.setProjectId(1L);
        example.setFieldId(10L);
        example.setExampleType("GOOD");
        example.setInput("统计已支付订单金额");
        example.setReason("使用支付成功过滤");
        example.setUpdatedAt(LocalDateTime.of(2026, 1, 4, 10, 0));
        when(usageExampleService.selectForAiContext(eq(1L), anyList(), isNull(), eq(50)))
                .thenReturn(List.of(example));
        FieldKnowledgeCardServiceImpl service = new FieldKnowledgeCardServiceImpl(
                fieldRepository, enumDictRepository, semanticRuleService,
                metricDefinitionService, usageExampleService, objectMapper);

        FieldKnowledgeCardResp card = service.get(1L, 10L);

        assertThat(card.semanticRules()).containsExactly(semanticRule);
        assertThat(card.enumHints()).extracting(EnumValueHint::value)
                .containsExactly("PAID", "LEGACY");
        assertThat(card.enumHints()).anySatisfy(hint -> {
            assertThat(hint.status()).isEqualTo("deprecated");
            assertThat(hint.replacementValue()).isEqualTo("PAID");
            assertThat(hint.aliases()).containsExactly("legacy_paid");
            assertThat(hint.mappingHints()).isEqualTo("旧系统支付成功映射到 PAID");
        });
        assertThat(card.metricReferences()).containsExactly(metric);
        assertThat(card.relatedFieldRefs()).contains("field:1:11");
        assertThat(card.usageExamples()).anyMatch(item -> item.contains("统计已支付订单金额"));
        assertThat(card.riskNotes()).anyMatch(note -> note.contains("单位换算: cent -> yuan"));
        assertThat(card.riskNotes()).anyMatch(note -> note.contains("枚举值 LEGACY 已deprecated，替代值: PAID"));
        assertThat(card.riskNotes()).anyMatch(note -> note.contains("指标 order_amount 有独立过滤/聚合/时间粒度口径"));
        assertThat(card.evidenceRefs()).contains(
                "semantic-doc:amount",
                "field-semantic-rule:5",
                "metric-doc:order",
                "metric-definition:order_amount",
                "usage-example:100");
        assertThat(card.lastVerifiedAt()).isEqualTo(LocalDateTime.of(2026, 1, 4, 10, 0));
    }

    @Test
    void list_usesBoundedRepositoryCandidateQueryAndRelatedEvidenceLoaders() {
        FieldRepository fieldRepository = mock(FieldRepository.class);
        EnumDictRepository enumDictRepository = mock(EnumDictRepository.class);
        FieldSemanticRuleService semanticRuleService = mock(FieldSemanticRuleService.class);
        MetricDefinitionService metricDefinitionService = mock(MetricDefinitionService.class);
        StandardUsageExampleService usageExampleService = mock(StandardUsageExampleService.class);
        Field amount = field(10L, "amount_cent");
        when(fieldRepository.findKnowledgeCardCandidates(1L, "amount", "enabled", 1)).thenReturn(List.of(amount));
        when(fieldRepository.countKnowledgeCardCandidates(1L, "amount", "enabled")).thenReturn(2L);
        when(semanticRuleService.listRelatedToFields(1L, List.of(10L), 50)).thenReturn(List.of());
        when(metricDefinitionService.listRelatedToFields(1L, List.of(10L), 50)).thenReturn(List.of());
        when(usageExampleService.selectForAiContext(eq(1L), anyList(), eq("amount"), eq(50))).thenReturn(List.of());
        FieldKnowledgeCardServiceImpl service = new FieldKnowledgeCardServiceImpl(
                fieldRepository, enumDictRepository, semanticRuleService,
                metricDefinitionService, usageExampleService, objectMapper);

        var result = service.list(1L, "amount", "enabled", null, 1);

        assertThat(result.returnedCount()).isEqualTo(1);
        assertThat(result.totalMatched()).isEqualTo(2);
        assertThat(result.truncated()).isTrue();
        verify(fieldRepository, never()).findAllByProjectId(1L);
        verify(fieldRepository).findKnowledgeCardCandidates(1L, "amount", "enabled", 1);
        verify(semanticRuleService).listRelatedToFields(1L, List.of(10L), 50);
        verify(metricDefinitionService).listRelatedToFields(1L, List.of(10L), 50);
    }

    @Test
    void list_preservesEvidenceForLaterScopedFieldsWhenFirstFieldWouldExhaustGlobalLimit() {
        FieldRepository fieldRepository = mock(FieldRepository.class);
        EnumDictRepository enumDictRepository = mock(EnumDictRepository.class);
        FieldSemanticRuleService semanticRuleService = mock(FieldSemanticRuleService.class);
        MetricDefinitionService metricDefinitionService = mock(MetricDefinitionService.class);
        StandardUsageExampleService usageExampleService = mock(StandardUsageExampleService.class);
        Field first = field(10L, "first_amount");
        Field second = field(11L, "second_amount");
        when(fieldRepository.findKnowledgeCardCandidates(1L, null, null, 2)).thenReturn(List.of(first, second));
        when(fieldRepository.countKnowledgeCardCandidates(1L, null, null)).thenReturn(2L);
        when(usageExampleService.selectForAiContext(eq(1L), anyList(), isNull(), eq(50))).thenReturn(List.of());
        List<FieldSemanticRuleResp> firstFieldRules = java.util.stream.LongStream.rangeClosed(1, 50)
                .mapToObj(id -> semanticRule(id, 10L, null))
                .toList();
        FieldSemanticRuleResp secondFieldRule = semanticRule(99L, 11L, null);
        when(semanticRuleService.listRelatedToFields(eq(1L), argThat(ids -> ids.equals(List.of(10L, 11L))), eq(50)))
                .thenReturn(firstFieldRules);
        when(semanticRuleService.listRelatedToFields(1L, List.of(10L), 50)).thenReturn(firstFieldRules);
        when(semanticRuleService.listRelatedToFields(1L, List.of(11L), 50)).thenReturn(List.of(secondFieldRule));
        MetricDefinitionResp firstMetric = metric(1L, "first_metric", List.of(10L));
        MetricDefinitionResp secondMetric = metric(2L, "second_metric", List.of(11L));
        when(metricDefinitionService.listRelatedToFields(eq(1L), argThat(ids -> ids.equals(List.of(10L, 11L))), eq(50)))
                .thenReturn(List.of(firstMetric));
        when(metricDefinitionService.listRelatedToFields(1L, List.of(10L), 50)).thenReturn(List.of(firstMetric));
        when(metricDefinitionService.listRelatedToFields(1L, List.of(11L), 50)).thenReturn(List.of(secondMetric));
        FieldKnowledgeCardServiceImpl service = new FieldKnowledgeCardServiceImpl(
                fieldRepository, enumDictRepository, semanticRuleService,
                metricDefinitionService, usageExampleService, objectMapper);

        var result = service.list(1L, null, null, null, 2);

        FieldKnowledgeCardResp secondCard = result.cards().get(1);
        assertThat(secondCard.fieldId()).isEqualTo(11L);
        assertThat(secondCard.semanticRules()).containsExactly(secondFieldRule);
        assertThat(secondCard.metricReferences()).containsExactly(secondMetric);
    }

    private Field field(Long id, String name) {
        Field field = new Field();
        field.setId(id);
        field.setProjectId(1L);
        field.setName(name);
        field.setDisplayName(name);
        field.setStatus("enabled");
        return field;
    }

    private EnumDict enumDict(Long id, Long projectId) {
        EnumDict dict = new EnumDict();
        dict.setId(id);
        dict.setProjectId(projectId);
        dict.setCode("payment_status");
        return dict;
    }

    private EnumValue enumValue(String value, String label, String status, String replacementValue, Integer sortOrder) {
        EnumValue enumValue = new EnumValue();
        enumValue.setValue(value);
        enumValue.setLabel(label);
        enumValue.setStatus(status);
        enumValue.setAliasesJson("LEGACY".equals(value) ? "[\"legacy_paid\"]" : null);
        enumValue.setReplacementValue(replacementValue);
        enumValue.setValidFrom(LocalDate.of(2020, 1, 1));
        enumValue.setValidTo("LEGACY".equals(value) ? LocalDate.of(2025, 12, 31) : null);
        enumValue.setMappingHints("LEGACY".equals(value) ? "旧系统支付成功映射到 PAID" : null);
        enumValue.setAiUsageNotes("LEGACY".equals(value) ? "新 SQL 不要继续使用旧值" : null);
        enumValue.setSortOrder(sortOrder);
        return enumValue;
    }

    private FieldSemanticRuleResp semanticRule(Long id, Long fieldId, Long sourceFieldId) {
        return new FieldSemanticRuleResp(
                id,
                1L,
                fieldId,
                sourceFieldId,
                "DERIVED",
                null,
                null,
                null,
                null,
                "use field " + fieldId,
                null,
                List.of("semantic-doc:" + id),
                "enabled",
                null,
                LocalDateTime.of(2026, 1, 1, 10, 0));
    }

    private MetricDefinitionResp metric(Long id, String metricKey, List<Long> measureFieldIds) {
        return new MetricDefinitionResp(
                id,
                1L,
                metricKey,
                metricKey,
                "metric for " + metricKey,
                measureFieldIds,
                List.of(),
                null,
                "sum",
                "day",
                null,
                null,
                List.of("metric-doc:" + id),
                "enabled",
                null,
                LocalDateTime.of(2026, 1, 1, 10, 0));
    }
}
