package com.dataspec.fieldhistory.service.impl;

import com.dataspec.changelog.entity.StandardChangeLog;
import com.dataspec.changelog.repository.StandardChangeLogRepository;
import com.dataspec.field.entity.Field;
import com.dataspec.fieldhistory.model.FieldHistoricalAlias;
import com.dataspec.fieldhistory.service.FieldHistoricalAliasService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 基于字段变更日志 JSON 快照的历史名称索引实现。
 *
 * <p>只读取 name、displayName 和 aliases 白名单字段。损坏快照按日志 ID 跳过，
 * 不记录或返回原始 JSON，避免历史快照中的业务内容或秘密进入日志。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FieldHistoricalAliasServiceImpl implements FieldHistoricalAliasService {

    private final StandardChangeLogRepository standardChangeLogRepository;
    private final ObjectMapper objectMapper;

    @Override
    public Map<Long, List<FieldHistoricalAlias>> load(Long projectId, List<Field> currentFields) {
        if (projectId == null || currentFields == null || currentFields.isEmpty()) {
            return Map.of();
        }
        Map<Long, Set<String>> currentValues = currentValues(projectId, currentFields);
        if (currentValues.isEmpty()) {
            return Map.of();
        }

        Map<Long, LinkedHashMap<String, FieldHistoricalAlias>> aliasesByField = new LinkedHashMap<>();
        for (StandardChangeLog logEntry : standardChangeLogRepository.findFieldHistoryByProjectId(projectId)) {
            Long fieldId = logEntry.getTargetId();
            Set<String> fieldCurrentValues = currentValues.get(fieldId);
            if (fieldCurrentValues == null) {
                continue;
            }
            collectSnapshot(logEntry, logEntry.getBeforeJson(), fieldCurrentValues, aliasesByField);
            collectSnapshot(logEntry, logEntry.getAfterJson(), fieldCurrentValues, aliasesByField);
        }

        Map<Long, List<FieldHistoricalAlias>> result = new LinkedHashMap<>();
        aliasesByField.forEach((fieldId, aliases) -> result.put(fieldId, List.copyOf(aliases.values())));
        return Map.copyOf(result);
    }

    private Map<Long, Set<String>> currentValues(Long projectId, List<Field> currentFields) {
        Map<Long, Set<String>> result = new LinkedHashMap<>();
        for (Field field : currentFields) {
            if (field == null || field.getId() == null || !Objects.equals(projectId, field.getProjectId())) {
                continue;
            }
            Set<String> values = new LinkedHashSet<>();
            addNormalized(values, field.getName());
            addNormalized(values, field.getDisplayName());
            for (String alias : splitCsv(field.getAliases())) {
                addNormalized(values, alias);
            }
            result.put(field.getId(), values);
        }
        return result;
    }

    private void collectSnapshot(
            StandardChangeLog logEntry,
            String snapshotJson,
            Set<String> currentValues,
            Map<Long, LinkedHashMap<String, FieldHistoricalAlias>> aliasesByField
    ) {
        if (snapshotJson == null || snapshotJson.isBlank()) {
            return;
        }
        try {
            JsonNode snapshot = objectMapper.readTree(snapshotJson);
            collectValue(logEntry, text(snapshot, "name"), currentValues, aliasesByField);
            collectValue(logEntry, text(snapshot, "displayName"), currentValues, aliasesByField);
            for (String alias : splitCsv(text(snapshot, "aliases"))) {
                collectValue(logEntry, alias, currentValues, aliasesByField);
            }
        } catch (Exception ex) {
            log.warn("字段历史快照解析失败，已跳过 changeLogId={}", logEntry.getId());
        }
    }

    private void collectValue(
            StandardChangeLog logEntry,
            String value,
            Set<String> currentValues,
            Map<Long, LinkedHashMap<String, FieldHistoricalAlias>> aliasesByField
    ) {
        String normalized = normalize(value);
        if (normalized.isEmpty() || currentValues.contains(normalized)) {
            return;
        }
        aliasesByField
                .computeIfAbsent(logEntry.getTargetId(), ignored -> new LinkedHashMap<>())
                .putIfAbsent(normalized, new FieldHistoricalAlias(logEntry.getTargetId(), value.trim(), logEntry.getId()));
    }

    private String text(JsonNode node, String fieldName) {
        if (node == null || !node.isObject()) {
            return null;
        }
        JsonNode value = node.get(fieldName);
        return value != null && value.isTextual() ? value.asText() : null;
    }

    private void addNormalized(Set<String> values, String value) {
        String normalized = normalize(value);
        if (!normalized.isEmpty()) {
            values.add(normalized);
        }
    }

    private List<String> splitCsv(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        List<String> values = new ArrayList<>();
        for (String part : value.split("[,，]")) {
            if (!part.isBlank()) {
                values.add(part.trim());
            }
        }
        return values;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
