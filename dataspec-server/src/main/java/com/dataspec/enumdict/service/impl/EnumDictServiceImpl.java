package com.dataspec.enumdict.service.impl;

import com.dataspec.changelog.service.StandardChangeLogService;
import com.dataspec.common.exception.BizException;
import com.dataspec.common.sanitize.SensitiveDataSanitizer;
import com.dataspec.enumdict.entity.EnumDict;
import com.dataspec.enumdict.entity.EnumValue;
import com.dataspec.enumdict.repository.EnumDictRepository;
import com.dataspec.enumdict.service.EnumDictService;
import com.dataspec.security.context.ProjectAccessGuard;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 枚举字典服务实现
 */

@Service
@RequiredArgsConstructor
public class EnumDictServiceImpl implements EnumDictService {

    private static final String STATUS_DRAFT = "draft";
    private static final String STATUS_ENABLED = "enabled";
    private static final String STATUS_DEPRECATED = "deprecated";
    private static final String STATUS_DISABLED = "disabled";
    private static final Set<String> ALLOWED_STATUSES = Set.of(STATUS_DRAFT, STATUS_ENABLED, STATUS_DEPRECATED, STATUS_DISABLED);
    private static final Pattern PRIVATE_KEY_PATTERN = Pattern.compile(
            "(?i)-----BEGIN [A-Z ]*PRIVATE KEY-----|\\bprivate[_-]?key\\s*[:=]");

    private final EnumDictRepository enumDictRepository;
    private final StandardChangeLogService changeLogService;
    private final ObjectMapper objectMapper;

    @Override
    public List<EnumDict> listByProject(Long projectId) {
        ProjectAccessGuard.requireProjectAccess(projectId);
        return enumDictRepository.findDictsByProjectId(projectId);
    }

    @Override
    public EnumDict getById(Long id) {
        EnumDict enumDict = enumDictRepository.findDictById(id)
                .orElseThrow(() -> new BizException("枚举字典不存在: " + id));
        ProjectAccessGuard.requireProjectAccess(enumDict.getProjectId());
        return enumDict;
    }

    @Override
    public EnumDict create(EnumDict enumDict) {
        ProjectAccessGuard.requireProjectAccess(enumDict.getProjectId());
        if (enumDictRepository.existsDictByCodeInProject(enumDict.getCode(), enumDict.getProjectId())) {
            throw new BizException("枚举编码已存在: " + enumDict.getCode());
        }
        enumDict.setValueType(enumDict.getValueType() != null ? enumDict.getValueType() : "integer");
        enumDictRepository.insertDict(enumDict);
        changeLogService.recordChange(
                enumDict.getProjectId(),
                StandardChangeLogService.TARGET_ENUM_DICT,
                enumDict.getId(),
                StandardChangeLogService.ACTION_CREATE,
                null,
                changeLogService.snapshot(enumDict));
        return enumDict;
    }

    @Override
    public EnumDict update(Long id, EnumDict enumDict) {
        EnumDict existing = getById(id);
        String beforeJson = changeLogService.snapshot(existing);
        existing.setName(enumDict.getName());
        existing.setCode(enumDict.getCode());
        existing.setDescription(enumDict.getDescription());
        existing.setValueType(enumDict.getValueType());
        enumDictRepository.updateDict(existing);
        changeLogService.recordChange(
                existing.getProjectId(),
                StandardChangeLogService.TARGET_ENUM_DICT,
                existing.getId(),
                StandardChangeLogService.ACTION_UPDATE,
                beforeJson,
                changeLogService.snapshot(existing));
        return existing;
    }

    @Override
    @Transactional
    public void delete(Long id) {
        EnumDict existing = getById(id);
        String beforeJson = changeLogService.snapshot(existing);
        enumDictRepository.deleteValuesByEnumId(id);
        enumDictRepository.deleteDictById(id);
        changeLogService.recordChange(
                existing.getProjectId(),
                StandardChangeLogService.TARGET_ENUM_DICT,
                existing.getId(),
                StandardChangeLogService.ACTION_DELETE,
                beforeJson,
                null);
    }

    @Override
    public List<EnumValue> listValues(Long enumId) {
        getById(enumId);
        return enumDictRepository.findValuesByEnumId(enumId);
    }

    @Override
    public EnumValue createValue(EnumValue value) {
        EnumDict enumDict = getById(value.getEnumId());
        applyEnumValueDefaults(value);
        enumDictRepository.insertValue(value);
        changeLogService.recordChange(
                enumDict.getProjectId(),
                StandardChangeLogService.TARGET_ENUM_VALUE,
                value.getId(),
                StandardChangeLogService.ACTION_CREATE,
                null,
                changeLogService.snapshot(value));
        return value;
    }

    @Override
    public EnumValue updateValue(Long id, EnumValue value) {
        EnumValue existing = getValueById(id);
        EnumDict enumDict = getById(existing.getEnumId());
        String beforeJson = changeLogService.snapshot(existing);
        existing.setValue(value.getValue());
        existing.setLabel(value.getLabel());
        existing.setSortOrder(value.getSortOrder());
        if (hasLifecycleInput(value)) {
            copyLifecycle(value, existing);
        }
        applyEnumValueDefaults(existing);
        enumDictRepository.updateValue(existing);
        changeLogService.recordChange(
                enumDict.getProjectId(),
                StandardChangeLogService.TARGET_ENUM_VALUE,
                id,
                StandardChangeLogService.ACTION_UPDATE,
                beforeJson,
                changeLogService.snapshot(existing));
        return existing;
    }

    @Override
    public void deleteValue(Long id) {
        EnumValue existing = getValueById(id);
        EnumDict enumDict = getById(existing.getEnumId());
        String beforeJson = changeLogService.snapshot(existing);
        enumDictRepository.deleteValueById(id);
        changeLogService.recordChange(
                enumDict.getProjectId(),
                StandardChangeLogService.TARGET_ENUM_VALUE,
                id,
                StandardChangeLogService.ACTION_DELETE,
                beforeJson,
                null);
    }

    private EnumValue getValueById(Long id) {
        EnumValue value = enumDictRepository.findValueById(id)
                .orElseThrow(() -> new BizException("枚举值不存在: " + id));
        EnumDict enumDict = getById(value.getEnumId());
        ProjectAccessGuard.requireProjectAccess(enumDict.getProjectId());
        return value;
    }

    private boolean hasLifecycleInput(EnumValue value) {
        return value.getStatus() != null
                || value.getAliasesJson() != null
                || value.getReplacementValue() != null
                || value.getValidFrom() != null
                || value.getValidTo() != null
                || value.getSourceEvidence() != null
                || value.getMappingHints() != null
                || value.getAiUsageNotes() != null;
    }

    private void copyLifecycle(EnumValue source, EnumValue target) {
        target.setStatus(source.getStatus());
        target.setAliasesJson(source.getAliasesJson());
        target.setReplacementValue(source.getReplacementValue());
        target.setValidFrom(source.getValidFrom());
        target.setValidTo(source.getValidTo());
        target.setSourceEvidence(source.getSourceEvidence());
        target.setMappingHints(source.getMappingHints());
        target.setAiUsageNotes(source.getAiUsageNotes());
    }

    private void applyEnumValueDefaults(EnumValue value) {
        value.setStatus(normalizeStatus(value.getStatus()));
        value.setAliasesJson(normalizeStringArrayJson(value.getAliasesJson(), "aliasesJson"));
        value.setReplacementValue(normalizeSafeText(value.getReplacementValue(), "replacementValue"));
        value.setSourceEvidence(normalizeSafeText(value.getSourceEvidence(), "sourceEvidence"));
        value.setMappingHints(normalizeSafeText(value.getMappingHints(), "mappingHints"));
        value.setAiUsageNotes(normalizeSafeText(value.getAiUsageNotes(), "aiUsageNotes"));
        if (value.getValidFrom() != null && value.getValidTo() != null && value.getValidFrom().isAfter(value.getValidTo())) {
            throw new BizException("validFrom 不能晚于 validTo");
        }
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
            throw new BizException("无效枚举值状态: " + status + "，允许值: " + ALLOWED_STATUSES);
        }
        return normalized;
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

    private String normalizeStringArrayJson(String value, String label) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String text = value.trim();
        if (SensitiveDataSanitizer.containsSensitiveText(text) || PRIVATE_KEY_PATTERN.matcher(text).find()) {
            throw new BizException(label + " 包含敏感连接或凭据信息，请改用脱敏说明");
        }
        try {
            JsonNode root = objectMapper.readTree(text);
            if (!root.isArray()) {
                throw new BizException(label + " 必须是 JSON 字符串数组");
            }
            List<String> values = new ArrayList<>();
            for (JsonNode item : root) {
                if (!item.isTextual()) {
                    throw new BizException(label + " 仅支持字符串值");
                }
                values.add(item.asText());
            }
            return values.isEmpty() ? null : objectMapper.writeValueAsString(values);
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException(label + " 必须是 JSON 字符串数组: " + e.getMessage());
        }
    }
}
