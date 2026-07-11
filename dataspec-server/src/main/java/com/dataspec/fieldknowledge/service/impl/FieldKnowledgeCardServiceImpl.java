package com.dataspec.fieldknowledge.service.impl;

import com.dataspec.common.exception.BizException;
import com.dataspec.common.sanitize.SensitiveDataSanitizer;
import com.dataspec.enumdict.entity.EnumDict;
import com.dataspec.enumdict.entity.EnumValue;
import com.dataspec.enumdict.repository.EnumDictRepository;
import com.dataspec.field.entity.Field;
import com.dataspec.field.repository.FieldRepository;
import com.dataspec.fieldknowledge.model.EnumValueHint;
import com.dataspec.fieldknowledge.model.FieldKnowledgeCardListResp;
import com.dataspec.fieldknowledge.model.FieldKnowledgeCardResp;
import com.dataspec.fieldknowledge.service.FieldKnowledgeCardService;
import com.dataspec.fieldsemantic.model.FieldSemanticRuleResp;
import com.dataspec.fieldsemantic.service.FieldSemanticRuleService;
import com.dataspec.metric.model.MetricDefinitionResp;
import com.dataspec.metric.service.MetricDefinitionService;
import com.dataspec.security.context.ProjectAccessGuard;
import com.dataspec.standardref.service.StandardReferenceFormatter;
import com.dataspec.standardusageexample.entity.StandardUsageExample;
import com.dataspec.standardusageexample.service.StandardUsageExampleService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 字段知识卡聚合服务实现。
 *
 * <p>知识卡是只读派生视图，不保存长卡片正文；每次读取从字段、语义规则、枚举、指标和示例等权威来源重新聚合，
 * 避免双源和过期内容。输出再次脱敏并裁剪，保证可安全进入 AI Context、前端和 CLI/MCP。</p>
 */
@Service
@RequiredArgsConstructor
public class FieldKnowledgeCardServiceImpl implements FieldKnowledgeCardService {

    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 50;
    private static final int TEXT_LIMIT = 180;
    private static final int JSON_TEXT_LIMIT = 240;
    private static final int LIST_ITEM_LIMIT = 8;
    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {
    };

    private final FieldRepository fieldRepository;
    private final EnumDictRepository enumDictRepository;
    private final FieldSemanticRuleService fieldSemanticRuleService;
    private final MetricDefinitionService metricDefinitionService;
    private final StandardUsageExampleService standardUsageExampleService;
    private final ObjectMapper objectMapper;

    @Override
    public FieldKnowledgeCardListResp list(Long projectId, String query, String status, Long fieldId, Integer limit) {
        requireProject(projectId);
        int safeLimit = safeLimit(limit);
        List<Field> selected;
        long totalMatched;
        if (fieldId == null) {
            selected = fieldRepository.findKnowledgeCardCandidates(projectId, query, status, safeLimit);
            totalMatched = fieldRepository.countKnowledgeCardCandidates(projectId, query, status);
        } else {
            List<Field> matched = List.of(requireField(projectId, fieldId)).stream()
                    .filter(field -> matchesStatus(field, status))
                    .filter(field -> matchesQuery(field, query))
                    .toList();
            selected = matched.stream().limit(safeLimit).toList();
            totalMatched = matched.size();
        }
        AggregationContext context = loadContext(projectId, selected, query);
        List<FieldKnowledgeCardResp> cards = selected.stream()
                .map(field -> buildCard(field, context))
                .toList();
        return new FieldKnowledgeCardListResp(
                projectId,
                Math.toIntExact(Math.min(totalMatched, Integer.MAX_VALUE)),
                cards.size(),
                totalMatched > cards.size(),
                cards);
    }

    @Override
    public FieldKnowledgeCardResp get(Long projectId, Long fieldId) {
        requireProject(projectId);
        Field field = requireField(projectId, fieldId);
        AggregationContext context = loadContext(projectId, List.of(field), null);
        return buildCard(field, context);
    }

    private void requireProject(Long projectId) {
        if (projectId == null || projectId <= 0) {
            throw new BizException("项目ID不能为空");
        }
        ProjectAccessGuard.requireProjectAccess(projectId);
    }

    private Field requireField(Long projectId, Long fieldId) {
        if (fieldId == null || fieldId <= 0) {
            throw new BizException("字段ID不能为空");
        }
        Field field = fieldRepository.findById(fieldId)
                .orElseThrow(() -> new BizException("字段不存在: " + fieldId));
        if (!Objects.equals(projectId, field.getProjectId())) {
            throw new BizException("字段不属于当前项目: " + fieldId);
        }
        return field;
    }

    private AggregationContext loadContext(Long projectId, List<Field> selectedFields, String query) {
        Set<Long> fieldIds = selectedFields.stream()
                .map(Field::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        List<Long> selectedFieldIds = new ArrayList<>(fieldIds);
        List<FieldSemanticRuleResp> semanticRules = loadSemanticRules(projectId, selectedFieldIds);
        List<MetricDefinitionResp> metrics = loadMetrics(projectId, selectedFieldIds);
        List<StandardUsageExample> examples = fieldIds.isEmpty()
                ? List.of()
                : standardUsageExampleService.selectForAiContext(projectId, selectedFieldIds, query, Math.max(MAX_LIMIT, fieldIds.size() * 4));
        Map<Long, List<EnumValueHint>> enumHintsByCodeSet = loadEnumHints(projectId, selectedFields);
        return new AggregationContext(
                groupSemanticRulesByField(semanticRules),
                groupIncomingSemanticRulesBySource(semanticRules),
                groupMetricsByField(metrics),
                groupExamplesByField(examples),
                enumHintsByCodeSet);
    }

    private List<FieldSemanticRuleResp> loadSemanticRules(Long projectId, List<Long> fieldIds) {
        if (fieldIds.isEmpty()) {
            return List.of();
        }
        if (fieldIds.size() == 1) {
            return fieldSemanticRuleService.listRelatedToFields(projectId, fieldIds, MAX_LIMIT);
        }
        // 多字段知识卡需要保证每个 scoped 字段都有机会拿到证据，避免前置字段较多规则时吃满全局 limit。
        Map<String, FieldSemanticRuleResp> unique = new LinkedHashMap<>();
        for (Long fieldId : fieldIds) {
            for (FieldSemanticRuleResp rule : fieldSemanticRuleService.listRelatedToFields(projectId, List.of(fieldId), MAX_LIMIT)) {
                unique.putIfAbsent(semanticRuleKey(rule), rule);
            }
        }
        return List.copyOf(unique.values());
    }

    private List<MetricDefinitionResp> loadMetrics(Long projectId, List<Long> fieldIds) {
        if (fieldIds.isEmpty()) {
            return List.of();
        }
        if (fieldIds.size() == 1) {
            return metricDefinitionService.listRelatedToFields(projectId, fieldIds, MAX_LIMIT);
        }
        // 指标引用同样按字段分批 bounded 查询，防止某个字段的指标过多导致后续字段卡片缺证据。
        Map<String, MetricDefinitionResp> unique = new LinkedHashMap<>();
        for (Long fieldId : fieldIds) {
            for (MetricDefinitionResp metric : metricDefinitionService.listRelatedToFields(projectId, List.of(fieldId), MAX_LIMIT)) {
                unique.putIfAbsent(metricKey(metric), metric);
            }
        }
        return List.copyOf(unique.values());
    }

    private String semanticRuleKey(FieldSemanticRuleResp rule) {
        if (rule.id() != null) {
            return "id:" + rule.id();
        }
        return "fields:%s:%s:%s".formatted(rule.fieldId(), rule.sourceFieldId(), rule.ruleType());
    }

    private String metricKey(MetricDefinitionResp metric) {
        if (metric.id() != null) {
            return "id:" + metric.id();
        }
        return "key:" + metric.metricKey();
    }

    private Map<Long, List<EnumValueHint>> loadEnumHints(Long projectId, List<Field> selectedFields) {
        Map<Long, List<EnumValueHint>> result = new LinkedHashMap<>();
        Set<Long> codeSetIds = selectedFields.stream()
                .map(Field::getCodeSetId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        for (Long codeSetId : codeSetIds) {
            Optional<EnumDict> dict = enumDictRepository.findDictById(codeSetId);
            if (dict.isEmpty() || !Objects.equals(projectId, dict.get().getProjectId())) {
                result.put(codeSetId, List.of());
                continue;
            }
            List<EnumValueHint> hints = enumDictRepository.findValuesByEnumId(codeSetId).stream()
                    .sorted(Comparator
                            .comparing((EnumValue value) -> enumStatusRank(value.getStatus()))
                            .thenComparing(EnumValue::getSortOrder, Comparator.nullsLast(Integer::compareTo))
                            .thenComparing(EnumValue::getValue, Comparator.nullsLast(String::compareTo)))
                    .limit(LIST_ITEM_LIMIT * 2L)
                    .map(this::toEnumHint)
                    .toList();
            result.put(codeSetId, hints);
        }
        return result;
    }

    private FieldKnowledgeCardResp buildCard(Field field, AggregationContext context) {
        List<FieldSemanticRuleResp> semanticRules = context.semanticRulesByField().getOrDefault(field.getId(), List.of());
        List<FieldSemanticRuleResp> incomingRules = context.incomingRulesBySourceField().getOrDefault(field.getId(), List.of());
        List<MetricDefinitionResp> metricReferences = context.metricsByField().getOrDefault(field.getId(), List.of());
        List<StandardUsageExample> examples = context.examplesByField().getOrDefault(field.getId(), List.of());
        List<EnumValueHint> enumHints = field.getCodeSetId() == null
                ? List.of()
                : context.enumHintsByCodeSet().getOrDefault(field.getCodeSetId(), List.of());
        List<String> relatedFieldRefs = relatedFieldRefs(field, semanticRules, incomingRules);
        List<String> riskNotes = riskNotes(field, semanticRules, enumHints, metricReferences);
        List<String> evidenceRefs = evidenceRefs(semanticRules, metricReferences, examples);
        return new FieldKnowledgeCardResp(
                field.getProjectId(),
                field.getId(),
                StandardReferenceFormatter.fieldRef(field.getProjectId(), field.getId()),
                safeText(field.getName()),
                safeText(field.getDisplayName()),
                safeText(field.getDataType()),
                safeText(field.getStatus()),
                splitCsv(field.getAliases()),
                formatSummary(field),
                usageContractSummary(field),
                namingGuidance(field),
                semanticRules,
                enumHints,
                usageExampleSummaries(examples),
                metricReferences,
                relatedFieldRefs,
                riskNotes,
                evidenceRefs,
                latestUpdatedAt(field, semanticRules, metricReferences, examples));
    }

    private Map<Long, List<FieldSemanticRuleResp>> groupSemanticRulesByField(List<FieldSemanticRuleResp> semanticRules) {
        return semanticRules.stream()
                .filter(rule -> rule.fieldId() != null)
                .collect(Collectors.groupingBy(
                        FieldSemanticRuleResp::fieldId,
                        LinkedHashMap::new,
                        Collectors.toList()));
    }

    private Map<Long, List<FieldSemanticRuleResp>> groupIncomingSemanticRulesBySource(List<FieldSemanticRuleResp> semanticRules) {
        return semanticRules.stream()
                .filter(rule -> rule.sourceFieldId() != null)
                .collect(Collectors.groupingBy(
                        FieldSemanticRuleResp::sourceFieldId,
                        LinkedHashMap::new,
                        Collectors.toList()));
    }

    private Map<Long, List<MetricDefinitionResp>> groupMetricsByField(List<MetricDefinitionResp> metrics) {
        Map<Long, List<MetricDefinitionResp>> result = new LinkedHashMap<>();
        for (MetricDefinitionResp metric : metrics) {
            for (Long fieldId : referencedFieldIds(metric)) {
                result.computeIfAbsent(fieldId, ignored -> new ArrayList<>()).add(metric);
            }
        }
        return result;
    }

    private Map<Long, List<StandardUsageExample>> groupExamplesByField(List<StandardUsageExample> examples) {
        return examples.stream()
                .filter(example -> example.getFieldId() != null)
                .collect(Collectors.groupingBy(
                        StandardUsageExample::getFieldId,
                        LinkedHashMap::new,
                        Collectors.toList()));
    }

    private List<Long> referencedFieldIds(MetricDefinitionResp metric) {
        Set<Long> fieldIds = new LinkedHashSet<>();
        if (metric.measureFieldIds() != null) {
            fieldIds.addAll(metric.measureFieldIds());
        }
        if (metric.dimensionFieldIds() != null) {
            fieldIds.addAll(metric.dimensionFieldIds());
        }
        return fieldIds.stream().filter(Objects::nonNull).toList();
    }

    private EnumValueHint toEnumHint(EnumValue value) {
        return new EnumValueHint(
                safeText(value.getValue()),
                safeText(value.getLabel()),
                safeText(defaultText(value.getStatus(), "enabled")),
                readStringList(value.getAliasesJson()),
                safeText(value.getReplacementValue()),
                value.getValidFrom(),
                value.getValidTo(),
                safeText(value.getMappingHints()),
                safeText(value.getAiUsageNotes()));
    }

    private List<String> formatSummary(Field field) {
        List<String> items = new ArrayList<>();
        addText(items, "数据类型: " + nullToDash(field.getDataType()));
        if (field.getLength() != null) {
            addText(items, "长度: " + field.getLength());
        }
        if (field.getPrecisionVal() != null || field.getScaleVal() != null) {
            addText(items, "精度: " + nullToDash(field.getPrecisionVal()) + "/" + nullToDash(field.getScaleVal()));
        }
        if (field.getNullable() != null) {
            addText(items, "可空: " + field.getNullable());
        }
        addText(items, labelValue("默认值", field.getDefaultValue()));
        addText(items, labelValue("格式类型", field.getFormatType()));
        addText(items, labelValue("格式模式", field.getFormatPattern()));
        addText(items, labelValue("单位", field.getFormatUnit()));
        addText(items, labelValue("精度说明", field.getFormatPrecision()));
        addText(items, labelValue("时区", field.getFormatTimezone()));
        addText(items, labelValue("空值策略", field.getFormatNullPolicy()));
        addText(items, labelValue("格式备注", field.getFormatNotes()));
        addJsonListSummary(items, "正例", field.getValidExamplesJson());
        addJsonListSummary(items, "反例", field.getInvalidExamplesJson());
        return limit(items, LIST_ITEM_LIMIT + 4);
    }

    private List<String> usageContractSummary(Field field) {
        List<String> items = new ArrayList<>();
        addText(items, labelValue("推荐场景", field.getPreferredUseCases()));
        addText(items, labelValue("禁用/需确认场景", field.getAvoidWhen()));
        addText(items, labelValue("Join 提示", field.getJoinHints()));
        addText(items, labelValue("默认过滤", field.getDefaultFilters()));
        addText(items, labelValue("聚合提示", field.getAggregationHints()));
        addText(items, labelValue("替代指导", field.getReplacementGuidance()));
        addText(items, labelValue("常见误用", field.getMisuseExamples()));
        addText(items, labelValue("语义摘要", field.getSemanticSummary()));
        return limit(items, LIST_ITEM_LIMIT);
    }

    private List<String> namingGuidance(Field field) {
        List<String> items = new ArrayList<>();
        addText(items, jsonSummary("本地化名称", field.getLocalizedNamesJson()));
        addText(items, labelValue("推荐英文名", field.getPreferredEnglishName()));
        addText(items, listSummary("翻译别名", readStringList(field.getTranslationAliasesJson())));
        addText(items, listSummary("禁用翻译", readStringList(field.getForbiddenTranslationsJson())));
        addText(items, labelValue("翻译置信度", field.getTranslationConfidence()));
        addText(items, labelValue("命名说明", field.getTranslationNotes()));
        return limit(items, LIST_ITEM_LIMIT);
    }

    private List<String> usageExampleSummaries(List<StandardUsageExample> examples) {
        List<String> items = new ArrayList<>();
        for (StandardUsageExample example : examples) {
            String type = defaultText(example.getExampleType(), "EXAMPLE");
            String input = firstNonBlank(example.getInput(), example.getAntiPattern(), example.getExpectedOutput());
            String reason = firstNonBlank(example.getReason(), example.getTags());
            addText(items, "%s: %s%s".formatted(
                    type,
                    nullToDash(input),
                    reason == null ? "" : "；理由: " + reason));
            if (items.size() >= LIST_ITEM_LIMIT) {
                break;
            }
        }
        return items;
    }

    private List<String> relatedFieldRefs(Field field,
                                          List<FieldSemanticRuleResp> semanticRules,
                                          List<FieldSemanticRuleResp> incomingRules) {
        Set<String> refs = new LinkedHashSet<>();
        if (field.getReplacementFieldId() != null) {
            refs.add(StandardReferenceFormatter.fieldRef(field.getProjectId(), field.getReplacementFieldId()));
        }
        for (FieldSemanticRuleResp rule : semanticRules) {
            if (rule.sourceFieldId() != null) {
                refs.add(StandardReferenceFormatter.fieldRef(field.getProjectId(), rule.sourceFieldId()));
            }
        }
        for (FieldSemanticRuleResp rule : incomingRules) {
            if (rule.fieldId() != null) {
                refs.add(StandardReferenceFormatter.fieldRef(field.getProjectId(), rule.fieldId()));
            }
        }
        return limit(new ArrayList<>(refs), LIST_ITEM_LIMIT);
    }

    private List<String> riskNotes(Field field,
                                   List<FieldSemanticRuleResp> semanticRules,
                                   List<EnumValueHint> enumHints,
                                   List<MetricDefinitionResp> metricReferences) {
        List<String> notes = new ArrayList<>();
        if (Boolean.TRUE.equals(field.getSensitive())) {
            addText(notes, "敏感字段：示例值和 AI Context 输出需要保持脱敏。");
        }
        if ("deprecated".equalsIgnoreCase(field.getStatus()) || "disabled".equalsIgnoreCase(field.getStatus())) {
            addText(notes, "字段状态为 " + field.getStatus() + "，使用前需要确认替代字段或迁移说明。");
        }
        if (!readStringList(field.getForbiddenTranslationsJson()).isEmpty()) {
            addText(notes, "存在禁用翻译，AI 不应直接用这些命名生成字段。");
        }
        addText(notes, labelValue("禁用/需确认场景", field.getAvoidWhen()));
        addText(notes, labelValue("常见误用", field.getMisuseExamples()));
        for (FieldSemanticRuleResp rule : semanticRules) {
            addText(notes, labelValue("source-of-truth", rule.sourceOfTruth()));
            addText(notes, labelValue("单位换算", rule.unitConversion()));
            addText(notes, labelValue("反例", rule.antiPatterns()));
        }
        for (EnumValueHint hint : enumHints) {
            if ("deprecated".equalsIgnoreCase(hint.status()) || "disabled".equalsIgnoreCase(hint.status())) {
                addText(notes, "枚举值 %s 已%s%s".formatted(
                        nullToDash(hint.value()),
                        hint.status(),
                        hint.replacementValue() == null ? "" : "，替代值: " + hint.replacementValue()));
            }
        }
        for (MetricDefinitionResp metric : metricReferences) {
            if (metric.filterRule() != null || metric.aggregationRule() != null || metric.timeGrain() != null) {
                addText(notes, "指标 %s 有独立过滤/聚合/时间粒度口径，不能仅按字段名推断。".formatted(metric.metricKey()));
            }
        }
        return limit(notes, LIST_ITEM_LIMIT);
    }

    private List<String> evidenceRefs(List<FieldSemanticRuleResp> semanticRules,
                                      List<MetricDefinitionResp> metricReferences,
                                      List<StandardUsageExample> examples) {
        Set<String> refs = new LinkedHashSet<>();
        for (FieldSemanticRuleResp rule : semanticRules) {
            if (rule.evidenceRefs() != null) {
                refs.addAll(rule.evidenceRefs());
            }
            if (rule.id() != null) {
                refs.add("field-semantic-rule:" + rule.id());
            }
        }
        for (MetricDefinitionResp metric : metricReferences) {
            if (metric.evidenceRefs() != null) {
                refs.addAll(metric.evidenceRefs());
            }
            if (metric.metricKey() != null) {
                refs.add("metric-definition:" + metric.metricKey());
            }
        }
        for (StandardUsageExample example : examples) {
            if (example.getId() != null) {
                refs.add("usage-example:" + example.getId());
            }
        }
        return limit(refs.stream().map(this::safeText).toList(), LIST_ITEM_LIMIT * 2);
    }

    private LocalDateTime latestUpdatedAt(Field field,
                                          List<FieldSemanticRuleResp> semanticRules,
                                          List<MetricDefinitionResp> metricReferences,
                                          List<StandardUsageExample> examples) {
        LocalDateTime latest = field.getUpdatedAt();
        for (FieldSemanticRuleResp rule : semanticRules) {
            latest = max(latest, rule.updatedAt());
        }
        for (MetricDefinitionResp metric : metricReferences) {
            latest = max(latest, metric.updatedAt());
        }
        for (StandardUsageExample example : examples) {
            latest = max(latest, example.getUpdatedAt());
        }
        return latest;
    }

    private boolean matchesStatus(Field field, String status) {
        if (status == null || status.isBlank()) {
            return true;
        }
        return status.trim().equalsIgnoreCase(field.getStatus());
    }

    private boolean matchesQuery(Field field, String query) {
        if (query == null || query.isBlank()) {
            return true;
        }
        String normalized = query.toLowerCase(Locale.ROOT).trim();
        return searchableText(field).toLowerCase(Locale.ROOT).contains(normalized);
    }

    private String searchableText(Field field) {
        return String.join(" ",
                safeJoin(field.getName(), field.getDisplayName(), field.getComment(), field.getTags(), field.getAliases(),
                        field.getCategory(), field.getPreferredEnglishName(), field.getLocalizedNamesJson(),
                        field.getTranslationAliasesJson(), field.getForbiddenTranslationsJson(), field.getTranslationNotes(),
                        field.getSemanticSummary(), field.getPreferredUseCases(), field.getAvoidWhen(),
                        field.getAggregationHints(), field.getReplacementGuidance(), field.getMisuseExamples()));
    }

    private String safeJoin(String... values) {
        List<String> items = new ArrayList<>();
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                items.add(value);
            }
        }
        return String.join(" ", items);
    }

    private int safeLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_LIMIT;
        }
        return Math.max(1, Math.min(MAX_LIMIT, limit));
    }

    private int statusRank(String status) {
        if ("enabled".equalsIgnoreCase(status)) {
            return 0;
        }
        if ("draft".equalsIgnoreCase(status)) {
            return 1;
        }
        if ("deprecated".equalsIgnoreCase(status)) {
            return 2;
        }
        if ("disabled".equalsIgnoreCase(status)) {
            return 3;
        }
        return 4;
    }

    private int enumStatusRank(String status) {
        if ("enabled".equalsIgnoreCase(status)) {
            return 0;
        }
        if ("draft".equalsIgnoreCase(status)) {
            return 1;
        }
        if ("deprecated".equalsIgnoreCase(status)) {
            return 2;
        }
        if ("disabled".equalsIgnoreCase(status)) {
            return 3;
        }
        return 4;
    }

    private void addJsonListSummary(List<String> items, String label, String json) {
        List<String> values = readStringList(json);
        if (!values.isEmpty()) {
            addText(items, listSummary(label, limit(values, 3)));
        }
    }

    private String jsonSummary(String label, String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            JsonNode node = objectMapper.readTree(json);
            return label + ": " + SensitiveDataSanitizer.redactText(
                    objectMapper.writeValueAsString(SensitiveDataSanitizer.sanitizeValue(objectMapper.convertValue(node, Object.class))),
                    JSON_TEXT_LIMIT);
        } catch (Exception e) {
            return labelValue(label, json);
        }
    }

    private List<String> readStringList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            JsonNode parsed = objectMapper.readTree(json);
            if (parsed.isArray()) {
                return objectMapper.convertValue(parsed, STRING_LIST).stream()
                        .map(this::safeText)
                        .filter(Objects::nonNull)
                        .toList();
            }
            if (parsed.isObject()) {
                List<String> values = new ArrayList<>();
                parsed.fields().forEachRemaining(entry -> addText(values, entry.getKey() + "=" + entry.getValue().asText()));
                return values;
            }
        } catch (Exception ignored) {
            String safe = safeText(json);
            return safe == null ? List.of() : List.of(safe);
        }
        String safe = safeText(json);
        return safe == null ? List.of() : List.of(safe);
    }

    private List<String> splitCsv(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        Set<String> items = new LinkedHashSet<>();
        for (String item : value.split(",")) {
            String safe = safeText(item);
            if (safe != null) {
                items.add(safe);
            }
        }
        return limit(new ArrayList<>(items), LIST_ITEM_LIMIT);
    }

    private void addText(List<String> items, String value) {
        String safe = safeText(value);
        if (safe != null && !items.contains(safe)) {
            items.add(safe);
        }
    }

    private String labelValue(String label, String value) {
        String safe = safeText(value);
        return safe == null ? null : label + ": " + safe;
    }

    private String listSummary(String label, List<String> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        return label + ": " + String.join(", ", limit(values, LIST_ITEM_LIMIT));
    }

    private String safeText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return SensitiveDataSanitizer.redactText(value.trim(), TEXT_LIMIT);
    }

    private String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private String nullToDash(Object value) {
        return value == null ? "-" : String.valueOf(value);
    }

    private <T> List<T> limit(List<T> items, int limit) {
        if (items == null || items.size() <= limit) {
            return items == null ? List.of() : List.copyOf(items);
        }
        return List.copyOf(items.subList(0, limit));
    }

    private LocalDateTime max(LocalDateTime left, LocalDateTime right) {
        if (left == null) {
            return right;
        }
        if (right == null) {
            return left;
        }
        return left.isAfter(right) ? left : right;
    }

    private record AggregationContext(
            Map<Long, List<FieldSemanticRuleResp>> semanticRulesByField,
            Map<Long, List<FieldSemanticRuleResp>> incomingRulesBySourceField,
            Map<Long, List<MetricDefinitionResp>> metricsByField,
            Map<Long, List<StandardUsageExample>> examplesByField,
            Map<Long, List<EnumValueHint>> enumHintsByCodeSet
    ) {
    }
}
