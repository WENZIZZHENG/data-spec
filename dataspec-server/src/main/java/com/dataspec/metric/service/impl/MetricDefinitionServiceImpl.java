package com.dataspec.metric.service.impl;

import com.dataspec.common.exception.BizException;
import com.dataspec.common.sanitize.SensitiveDataSanitizer;
import com.dataspec.field.entity.Field;
import com.dataspec.field.repository.FieldRepository;
import com.dataspec.metric.entity.MetricDefinition;
import com.dataspec.metric.model.MetricDefinitionReq;
import com.dataspec.metric.model.MetricDefinitionResp;
import com.dataspec.metric.repository.MetricDefinitionRepository;
import com.dataspec.metric.service.MetricDefinitionService;
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
 * 指标口径服务实现。
 */
@Service
@RequiredArgsConstructor
public class MetricDefinitionServiceImpl implements MetricDefinitionService {

    private static final String STATUS_DRAFT = "draft";
    private static final String STATUS_ENABLED = "enabled";
    private static final String STATUS_DEPRECATED = "deprecated";
    private static final String STATUS_DISABLED = "disabled";
    private static final Set<String> ALLOWED_STATUSES = Set.of(STATUS_DRAFT, STATUS_ENABLED, STATUS_DEPRECATED, STATUS_DISABLED);
    private static final Pattern METRIC_KEY_PATTERN = Pattern.compile("[a-z][a-z0-9_]{1,99}");
    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 100;
    private static final Pattern PRIVATE_KEY_PATTERN = Pattern.compile(
            "(?i)-----BEGIN [A-Z ]*PRIVATE KEY-----|\\bprivate[_-]?key\\s*[:=]");
    private static final TypeReference<List<Long>> LONG_LIST = new TypeReference<>() {
    };
    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {
    };

    private final MetricDefinitionRepository repository;
    private final FieldRepository fieldRepository;
    private final ObjectMapper objectMapper;

    @Override
    public List<MetricDefinitionResp> list(
            Long projectId,
            String query,
            String status,
            Long fieldId,
            String metricKey,
            Integer limit) {
        if (projectId == null) {
            throw new BizException("项目ID不能为空");
        }
        ProjectAccessGuard.requireProjectAccess(projectId);
        if (fieldId != null) {
            requireFieldInProject(fieldId, projectId, "字段");
        }
        String normalizedStatus = normalizeOptionalStatus(status);
        String normalizedMetricKey = normalizeOptionalMetricKey(metricKey);
        return repository.findByProject(
                        projectId,
                        sanitizeQuery(query),
                        normalizedStatus,
                        fieldId,
                        normalizedMetricKey,
                        safeLimit(limit))
                .stream()
                .filter(definition -> fieldId == null || referencesField(definition, fieldId))
                .map(this::toResp)
                .toList();
    }

    @Override
    public List<MetricDefinitionResp> listRelatedToFields(Long projectId, List<Long> fieldIds, Integer limit) {
        if (projectId == null) {
            throw new BizException("项目ID不能为空");
        }
        ProjectAccessGuard.requireProjectAccess(projectId);
        List<Long> ids = uniquePositiveIds(fieldIds, "fieldIds");
        if (ids.isEmpty()) {
            return List.of();
        }
        for (Long fieldId : ids) {
            requireFieldInProject(fieldId, projectId, "fieldIds");
        }
        return repository.findRelatedToFields(projectId, ids, safeLimit(limit)).stream()
                .map(this::toResp)
                .toList();
    }

    @Override
    public MetricDefinitionResp getById(Long id) {
        MetricDefinition definition = getDefinition(id);
        ProjectAccessGuard.requireProjectAccess(definition.getProjectId());
        return toResp(definition);
    }

    @Override
    @Transactional
    public MetricDefinitionResp create(MetricDefinitionReq req) {
        validateReq(req, null);
        MetricDefinition definition = new MetricDefinition();
        definition.setProjectId(req.projectId());
        applyReq(definition, req);
        repository.insert(definition);
        return toResp(definition);
    }

    @Override
    @Transactional
    public MetricDefinitionResp update(Long id, MetricDefinitionReq req) {
        MetricDefinition existing = getDefinition(id);
        ProjectAccessGuard.requireProjectAccess(existing.getProjectId());
        if (req.projectId() != null && !Objects.equals(existing.getProjectId(), req.projectId())) {
            throw new BizException("指标口径不属于请求项目");
        }
        validateReq(new MetricDefinitionReq(
                existing.getProjectId(),
                req.metricKey(),
                req.displayName(),
                req.definition(),
                req.measureFieldIds(),
                req.dimensionFieldIds(),
                req.filterRule(),
                req.aggregationRule(),
                req.timeGrain(),
                req.ownerNotes(),
                req.exampleSql(),
                req.evidenceRefs(),
                req.status()), id);
        applyReq(existing, req);
        repository.update(existing);
        return toResp(existing);
    }

    @Override
    public void delete(Long id) {
        MetricDefinition existing = getDefinition(id);
        ProjectAccessGuard.requireProjectAccess(existing.getProjectId());
        repository.deleteById(id);
    }

    private MetricDefinition getDefinition(Long id) {
        if (id == null || id <= 0) {
            throw new BizException("无效指标口径ID: " + id);
        }
        return repository.findById(id)
                .orElseThrow(() -> new BizException("指标口径不存在: " + id));
    }

    private void validateReq(MetricDefinitionReq req, Long excludeId) {
        if (req == null || req.projectId() == null) {
            throw new BizException("项目ID不能为空");
        }
        ProjectAccessGuard.requireProjectAccess(req.projectId());
        String metricKey = normalizeMetricKey(req.metricKey());
        if (repository.existsByMetricKey(req.projectId(), metricKey, excludeId)) {
            throw new BizException("项目内指标键已存在: " + metricKey);
        }
        validateFieldRefs(req.measureFieldIds(), req.projectId(), "measureFieldIds");
        validateFieldRefs(req.dimensionFieldIds(), req.projectId(), "dimensionFieldIds");
    }

    private void validateFieldRefs(List<Long> fieldIds, Long projectId, String label) {
        for (Long fieldId : uniquePositiveIds(fieldIds, label)) {
            requireFieldInProject(fieldId, projectId, label);
        }
    }

    private void requireFieldInProject(Long fieldId, Long projectId, String label) {
        Field field = fieldRepository.findById(fieldId)
                .orElseThrow(() -> new BizException(label + " 字段不存在: " + fieldId));
        if (!Objects.equals(projectId, field.getProjectId())) {
            throw new BizException(label + " 字段不属于当前项目: " + fieldId);
        }
    }

    private void applyReq(MetricDefinition definition, MetricDefinitionReq req) {
        definition.setMetricKey(normalizeMetricKey(req.metricKey()));
        definition.setDisplayName(normalizeSafeText(req.displayName(), "displayName"));
        definition.setDefinition(normalizeSafeText(req.definition(), "definition"));
        definition.setMeasureFieldsJson(writeLongList(req.measureFieldIds(), "measureFieldIds"));
        definition.setDimensionFieldsJson(writeLongList(req.dimensionFieldIds(), "dimensionFieldIds"));
        definition.setFilterRule(normalizeSafeText(req.filterRule(), "filterRule"));
        definition.setAggregationRule(normalizeSafeText(req.aggregationRule(), "aggregationRule"));
        definition.setTimeGrain(normalizeSafeText(req.timeGrain(), "timeGrain"));
        definition.setOwnerNotes(normalizeSafeText(req.ownerNotes(), "ownerNotes"));
        definition.setExampleSql(normalizeSafeText(req.exampleSql(), "exampleSql"));
        definition.setEvidenceRefsJson(writeStringList(req.evidenceRefs(), "evidenceRefs"));
        definition.setStatus(normalizeStatus(req.status()));
    }

    private String normalizeMetricKey(String value) {
        String normalized = normalizeSafeText(value, "metricKey");
        if (normalized == null || !METRIC_KEY_PATTERN.matcher(normalized).matches()) {
            throw new BizException("指标键必须为 2-100 位 snake_case 小写字母、数字或下划线，并以字母开头");
        }
        return normalized;
    }

    private String normalizeOptionalMetricKey(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return normalizeMetricKey(value);
    }

    private String normalizeOptionalStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        return normalizeStatus(status);
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
            throw new BizException("无效指标口径状态，允许值: " + ALLOWED_STATUSES);
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
                throw new BizException(label + " 包含无效字段ID: " + id);
            }
        }
        return ids;
    }

    private String writeLongList(List<Long> values, String label) {
        List<Long> ids = uniquePositiveIds(values, label);
        if (ids.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(ids);
        } catch (Exception e) {
            throw new BizException(label + " 序列化失败: " + e.getMessage());
        }
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

    private List<Long> readLongList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, LONG_LIST);
        } catch (Exception e) {
            return List.of();
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

    private boolean referencesField(MetricDefinition definition, Long fieldId) {
        return readLongList(definition.getMeasureFieldsJson()).contains(fieldId)
                || readLongList(definition.getDimensionFieldsJson()).contains(fieldId);
    }

    private MetricDefinitionResp toResp(MetricDefinition definition) {
        return new MetricDefinitionResp(
                definition.getId(),
                definition.getProjectId(),
                definition.getMetricKey(),
                definition.getDisplayName(),
                definition.getDefinition(),
                readLongList(definition.getMeasureFieldsJson()),
                readLongList(definition.getDimensionFieldsJson()),
                definition.getFilterRule(),
                definition.getAggregationRule(),
                definition.getTimeGrain(),
                definition.getOwnerNotes(),
                definition.getExampleSql(),
                readStringList(definition.getEvidenceRefsJson()),
                definition.getStatus(),
                definition.getCreatedAt(),
                definition.getUpdatedAt());
    }
}
