package com.dataspec.fieldsemantic.service.impl;

import com.dataspec.common.exception.BizException;
import com.dataspec.common.sanitize.SensitiveDataSanitizer;
import com.dataspec.field.entity.Field;
import com.dataspec.field.repository.FieldRepository;
import com.dataspec.fieldsemantic.entity.FieldSemanticRule;
import com.dataspec.fieldsemantic.model.FieldSemanticRuleReq;
import com.dataspec.fieldsemantic.model.FieldSemanticRuleResp;
import com.dataspec.fieldsemantic.repository.FieldSemanticRuleRepository;
import com.dataspec.fieldsemantic.service.FieldSemanticRuleService;
import com.dataspec.security.context.ProjectAccessGuard;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 字段语义规则服务实现。
 */
@Service
@RequiredArgsConstructor
public class FieldSemanticRuleServiceImpl implements FieldSemanticRuleService {

    private static final String STATUS_DRAFT = "draft";
    private static final String STATUS_ENABLED = "enabled";
    private static final String STATUS_DISABLED = "disabled";
    private static final Set<String> ALLOWED_STATUSES = Set.of(STATUS_DRAFT, STATUS_ENABLED, STATUS_DISABLED);
    private static final Set<String> ALLOWED_RULE_TYPES = Set.of(
            "DERIVED_FROM",
            "UNIT_CONVERSION",
            "AGGREGATION",
            "TIME_GRAIN",
            "SOURCE_OF_TRUTH",
            "NAMING");
    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 100;
    private static final Pattern PRIVATE_KEY_PATTERN = Pattern.compile(
            "(?i)-----BEGIN [A-Z ]*PRIVATE KEY-----|\\bprivate[_-]?key\\s*[:=]");
    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {
    };

    private final FieldSemanticRuleRepository repository;
    private final FieldRepository fieldRepository;
    private final ObjectMapper objectMapper;

    @Override
    public List<FieldSemanticRuleResp> list(Long projectId, Long fieldId, String ruleType, String query, Integer limit) {
        if (projectId == null) {
            throw new BizException("项目ID不能为空");
        }
        ProjectAccessGuard.requireProjectAccess(projectId);
        if (fieldId != null) {
            requireFieldInProject(fieldId, projectId, "字段");
        }
        String normalizedRuleType = normalizeOptionalRuleType(ruleType);
        return repository.findByProject(projectId, fieldId, normalizedRuleType, sanitizeQuery(query), safeLimit(limit)).stream()
                .map(this::toResp)
                .toList();
    }

    @Override
    public List<FieldSemanticRuleResp> listRelatedToFields(Long projectId, List<Long> fieldIds, Integer limit) {
        if (projectId == null) {
            throw new BizException("项目ID不能为空");
        }
        ProjectAccessGuard.requireProjectAccess(projectId);
        List<Long> ids = uniquePositiveIds(fieldIds, "fieldIds");
        if (ids.isEmpty()) {
            return List.of();
        }
        for (Long fieldId : ids) {
            requireFieldInProject(fieldId, projectId, "字段");
        }
        return repository.findRelatedToFields(projectId, ids, safeLimit(limit)).stream()
                .map(this::toResp)
                .toList();
    }

    @Override
    public FieldSemanticRuleResp getById(Long id) {
        FieldSemanticRule rule = getRule(id);
        ProjectAccessGuard.requireProjectAccess(rule.getProjectId());
        return toResp(rule);
    }

    @Override
    @Transactional
    public FieldSemanticRuleResp create(FieldSemanticRuleReq req) {
        validateReq(req);
        FieldSemanticRule rule = new FieldSemanticRule();
        rule.setProjectId(req.projectId());
        applyReq(rule, req);
        repository.insert(rule);
        return toResp(rule);
    }

    @Override
    @Transactional
    public FieldSemanticRuleResp update(Long id, FieldSemanticRuleReq req) {
        FieldSemanticRule existing = getRule(id);
        ProjectAccessGuard.requireProjectAccess(existing.getProjectId());
        if (req.projectId() != null && !Objects.equals(existing.getProjectId(), req.projectId())) {
            throw new BizException("语义规则不属于请求项目");
        }
        validateReq(new FieldSemanticRuleReq(
                existing.getProjectId(),
                req.fieldId(),
                req.sourceFieldId(),
                req.ruleType(),
                req.unitConversion(),
                req.aggregationRule(),
                req.timeGranularity(),
                req.sourceOfTruth(),
                req.recommendedUse(),
                req.antiPatterns(),
                req.evidenceRefs(),
                req.status()));
        applyReq(existing, req);
        repository.update(existing);
        return toResp(existing);
    }

    @Override
    public void delete(Long id) {
        FieldSemanticRule existing = getRule(id);
        ProjectAccessGuard.requireProjectAccess(existing.getProjectId());
        repository.deleteById(id);
    }

    private FieldSemanticRule getRule(Long id) {
        if (id == null || id <= 0) {
            throw new BizException("无效语义规则ID: " + id);
        }
        return repository.findById(id)
                .orElseThrow(() -> new BizException("字段语义规则不存在: " + id));
    }

    private void validateReq(FieldSemanticRuleReq req) {
        if (req == null || req.projectId() == null) {
            throw new BizException("项目ID不能为空");
        }
        ProjectAccessGuard.requireProjectAccess(req.projectId());
        requireFieldInProject(req.fieldId(), req.projectId(), "字段");
        if (req.sourceFieldId() != null) {
            requireFieldInProject(req.sourceFieldId(), req.projectId(), "源字段");
        }
    }

    private void requireFieldInProject(Long fieldId, Long projectId, String label) {
        if (fieldId == null || fieldId <= 0) {
            throw new BizException(label + "ID不能为空");
        }
        Field field = fieldRepository.findById(fieldId)
                .orElseThrow(() -> new BizException(label + "不存在: " + fieldId));
        if (!Objects.equals(projectId, field.getProjectId())) {
            throw new BizException(label + "不属于当前项目: " + fieldId);
        }
    }

    private void applyReq(FieldSemanticRule rule, FieldSemanticRuleReq req) {
        rule.setFieldId(req.fieldId());
        rule.setSourceFieldId(req.sourceFieldId());
        rule.setRuleType(normalizeRuleType(req.ruleType()));
        rule.setUnitConversion(normalizeSafeText(req.unitConversion(), "unitConversion"));
        rule.setAggregationRule(normalizeSafeText(req.aggregationRule(), "aggregationRule"));
        rule.setTimeGranularity(normalizeSafeText(req.timeGranularity(), "timeGranularity"));
        rule.setSourceOfTruth(normalizeSafeText(req.sourceOfTruth(), "sourceOfTruth"));
        rule.setRecommendedUse(normalizeSafeText(req.recommendedUse(), "recommendedUse"));
        rule.setAntiPatterns(normalizeSafeText(req.antiPatterns(), "antiPatterns"));
        rule.setEvidenceRefsJson(writeStringList(req.evidenceRefs(), "evidenceRefs"));
        rule.setStatus(normalizeStatus(req.status()));
    }

    private String normalizeOptionalRuleType(String ruleType) {
        if (ruleType == null || ruleType.isBlank()) {
            return null;
        }
        return normalizeRuleType(ruleType);
    }

    private String normalizeRuleType(String ruleType) {
        if (ruleType == null || ruleType.isBlank()) {
            throw new BizException("规则类型不能为空");
        }
        String normalized = ruleType.trim().toUpperCase(Locale.ROOT).replace('-', '_');
        if (SensitiveDataSanitizer.containsSensitiveText(normalized) || PRIVATE_KEY_PATTERN.matcher(normalized).find()) {
            throw new BizException("ruleType 包含敏感连接或凭据信息，请改用脱敏说明");
        }
        if (!ALLOWED_RULE_TYPES.contains(normalized)) {
            throw new BizException("无效语义规则类型，允许值: " + ALLOWED_RULE_TYPES);
        }
        return normalized;
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return STATUS_ENABLED;
        }
        String normalized = status.trim().toLowerCase(Locale.ROOT);
        if (SensitiveDataSanitizer.containsSensitiveText(normalized) || PRIVATE_KEY_PATTERN.matcher(normalized).find()) {
            throw new BizException("status 包含敏感连接或凭据信息，请改用脱敏说明");
        }
        if (!ALLOWED_STATUSES.contains(normalized)) {
            throw new BizException("无效语义规则状态，允许值: " + ALLOWED_STATUSES);
        }
        return normalized;
    }

    private String sanitizeQuery(String query) {
        String normalized = normalizeSafeText(query, "query");
        return normalized == null ? null : SensitiveDataSanitizer.redactText(normalized);
    }

    private int safeLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_LIMIT;
        }
        if (limit <= 0) {
            throw new BizException("limit 必须大于 0");
        }
        return Math.min(limit, MAX_LIMIT);
    }

    private String normalizeSafeText(String value, String label) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim();
        if (SensitiveDataSanitizer.containsSensitiveText(normalized) || PRIVATE_KEY_PATTERN.matcher(normalized).find()) {
            throw new BizException(label + " 包含敏感连接或凭据信息，请改用脱敏说明");
        }
        return normalized;
    }

    private List<Long> uniquePositiveIds(List<Long> fieldIds, String label) {
        if (fieldIds == null || fieldIds.isEmpty()) {
            return List.of();
        }
        List<Long> ids = new ArrayList<>(new LinkedHashSet<>(fieldIds));
        for (Long id : ids) {
            if (id == null || id <= 0) {
                throw new BizException(label + " 只能包含正整数 ID");
            }
        }
        return ids;
    }

    private String writeStringList(List<String> values, String label) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        List<String> normalized = new ArrayList<>();
        for (String value : values) {
            String text = normalizeSafeText(value, label);
            if (text != null && !normalized.contains(text)) {
                normalized.add(text);
            }
        }
        if (normalized.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(normalized);
        } catch (Exception e) {
            throw new BizException(label + " 序列化失败: " + e.getMessage());
        }
    }

    private List<String> readStringList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, STRING_LIST);
        } catch (Exception e) {
            return List.of();
        }
    }

    private FieldSemanticRuleResp toResp(FieldSemanticRule rule) {
        return new FieldSemanticRuleResp(
                rule.getId(),
                rule.getProjectId(),
                rule.getFieldId(),
                rule.getSourceFieldId(),
                rule.getRuleType(),
                rule.getUnitConversion(),
                rule.getAggregationRule(),
                rule.getTimeGranularity(),
                rule.getSourceOfTruth(),
                rule.getRecommendedUse(),
                rule.getAntiPatterns(),
                readStringList(rule.getEvidenceRefsJson()),
                rule.getStatus(),
                rule.getCreatedAt(),
                rule.getUpdatedAt());
    }
}
