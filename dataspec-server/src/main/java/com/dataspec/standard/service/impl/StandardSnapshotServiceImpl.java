package com.dataspec.standard.service.impl;

import com.dataspec.common.exception.BizException;
import com.dataspec.enumdict.entity.EnumDict;
import com.dataspec.enumdict.entity.EnumValue;
import com.dataspec.enumdict.service.EnumDictService;
import com.dataspec.field.entity.Field;
import com.dataspec.field.service.FieldService;
import com.dataspec.rule.entity.RuleConfig;
import com.dataspec.rule.service.RuleConfigService;
import com.dataspec.security.context.ProjectAccessGuard;
import com.dataspec.standard.dto.StandardSnapshotCreateReq;
import com.dataspec.standard.dto.StandardSnapshotInfo;
import com.dataspec.standard.dto.StandardSnapshotPayload;
import com.dataspec.standard.entity.StandardSnapshot;
import com.dataspec.standard.repository.StandardSnapshotRepository;
import com.dataspec.standard.service.StandardSnapshotService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 标准版本快照服务实现。
 */
@Service
@RequiredArgsConstructor
public class StandardSnapshotServiceImpl implements StandardSnapshotService {

    private static final int VERSION_MAX_LENGTH = 100;

    private final StandardSnapshotRepository standardSnapshotRepository;
    private final FieldService fieldService;
    private final EnumDictService enumDictService;
    private final RuleConfigService ruleConfigService;
    private final ObjectMapper objectMapper;

    @Override
    public StandardSnapshotInfo createSnapshot(Long projectId, StandardSnapshotCreateReq req) {
        requireProject(projectId);
        ProjectAccessGuard.requireProjectAccess(projectId);
        if (req == null) {
            throw new BizException(400, "标准快照请求不能为空");
        }
        String version = requiredVersion(req.version());
        if (standardSnapshotRepository.existsByProjectIdAndVersion(projectId, version)) {
            throw new BizException(400, "标准版本号已存在: " + version);
        }
        String payloadJson = buildPayloadJson(projectId);

        StandardSnapshot snapshot = new StandardSnapshot();
        snapshot.setProjectId(projectId);
        snapshot.setVersion(version);
        snapshot.setName(trimToNull(req.name()));
        snapshot.setDescription(trimToNull(req.description()));
        snapshot.setPayloadJson(payloadJson);
        snapshot.setSnapshotHash(sha256(payloadJson));
        standardSnapshotRepository.save(snapshot);
        return toInfo(snapshot).withSource("current");
    }

    @Override
    public StandardSnapshotInfo getCurrentSnapshot(Long projectId) {
        requireProject(projectId);
        ProjectAccessGuard.requireProjectAccess(projectId);
        return standardSnapshotRepository.findLatestByProjectId(projectId)
                .map(this::toInfo)
                .map(snapshot -> snapshot.withSource("current"))
                .orElseGet(() -> StandardSnapshotInfo.unversioned(projectId));
    }

    @Override
    public List<StandardSnapshotInfo> listSnapshots(Long projectId) {
        requireProject(projectId);
        ProjectAccessGuard.requireProjectAccess(projectId);
        return standardSnapshotRepository.findByProjectId(projectId).stream()
                .map(this::toInfo)
                .toList();
    }

    @Override
    public StandardSnapshotPayload getSnapshotPayload(Long projectId, Long snapshotId) {
        requireProject(projectId);
        ProjectAccessGuard.requireProjectAccess(projectId);
        if (snapshotId == null) {
            throw new BizException(400, "标准快照 ID 不能为空");
        }
        StandardSnapshot snapshot = standardSnapshotRepository.findByProjectIdAndId(projectId, snapshotId)
                .orElseThrow(() -> new BizException(404, "标准快照不存在或不属于当前项目: " + snapshotId));
        return toPayload(snapshot);
    }

    @Override
    public StandardSnapshotPayload getSnapshotPayloadByVersion(Long projectId, String version) {
        requireProject(projectId);
        ProjectAccessGuard.requireProjectAccess(projectId);
        String normalizedVersion = requiredVersion(version);
        StandardSnapshot snapshot = standardSnapshotRepository.findByProjectIdAndVersion(projectId, normalizedVersion)
                .orElseThrow(() -> new BizException(404, "标准版本不存在或不属于当前项目: " + normalizedVersion));
        return toPayload(snapshot);
    }

    private String buildPayloadJson(Long projectId) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("projectId", projectId);
            payload.put("fields", fieldService.listByProject(projectId).stream()
                    .sorted(Comparator
                            .comparing(Field::getName, Comparator.nullsLast(String::compareTo))
                            .thenComparing(Field::getId, Comparator.nullsLast(Long::compareTo)))
                    .map(this::fieldPayload)
                    .toList());
            payload.put("enums", enumDictService.listByProject(projectId).stream()
                    .sorted(Comparator
                            .comparing(EnumDict::getCode, Comparator.nullsLast(String::compareTo))
                            .thenComparing(EnumDict::getId, Comparator.nullsLast(Long::compareTo)))
                    .map(this::enumPayload)
                    .toList());
            payload.put("rules", ruleConfigService.listByProject(projectId).stream()
                    .sorted(Comparator
                            .comparing(RuleConfig::getRuleCode, Comparator.nullsLast(String::compareTo))
                            .thenComparing(RuleConfig::getId, Comparator.nullsLast(Long::compareTo)))
                    .map(this::rulePayload)
                    .toList());
            return objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            throw new BizException(500, "生成标准快照失败: " + e.getMessage());
        }
    }

    private Map<String, Object> fieldPayload(Field field) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", field.getId());
        item.put("name", field.getName());
        item.put("displayName", field.getDisplayName());
        item.put("dataType", field.getDataType());
        item.put("length", field.getLength());
        item.put("precisionVal", field.getPrecisionVal());
        item.put("scaleVal", field.getScaleVal());
        item.put("nullable", field.getNullable());
        item.put("defaultValue", field.getDefaultValue());
        item.put("comment", field.getComment());
        item.put("domainId", field.getDomainId());
        item.put("tags", field.getTags());
        item.put("aliases", field.getAliases());
        item.put("category", field.getCategory());
        item.put("codeSetId", field.getCodeSetId());
        item.put("sensitive", field.getSensitive());
        item.put("status", field.getStatus());
        item.put("exampleValue", field.getExampleValue());
        return item;
    }

    private Map<String, Object> enumPayload(EnumDict enumDict) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", enumDict.getId());
        item.put("code", enumDict.getCode());
        item.put("name", enumDict.getName());
        item.put("description", enumDict.getDescription());
        item.put("valueType", enumDict.getValueType());
        item.put("values", enumDictService.listValues(enumDict.getId()).stream()
                .sorted(Comparator
                        .comparing(EnumValue::getSortOrder, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(EnumValue::getValue, Comparator.nullsLast(String::compareTo))
                        .thenComparing(EnumValue::getId, Comparator.nullsLast(Long::compareTo)))
                .map(this::enumValuePayload)
                .toList());
        return item;
    }

    private Map<String, Object> enumValuePayload(EnumValue value) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", value.getId());
        item.put("value", value.getValue());
        item.put("label", value.getLabel());
        item.put("sortOrder", value.getSortOrder());
        return item;
    }

    private Map<String, Object> rulePayload(RuleConfig rule) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", rule.getId());
        item.put("ruleCode", rule.getRuleCode());
        item.put("ruleName", rule.getRuleName());
        item.put("severity", rule.getSeverity());
        item.put("enabled", rule.getEnabled());
        item.put("paramsJson", rule.getParamsJson());
        return item;
    }

    private StandardSnapshotInfo toInfo(StandardSnapshot snapshot) {
        return new StandardSnapshotInfo(
                snapshot.getId(),
                snapshot.getProjectId(),
                snapshot.getVersion(),
                snapshot.getName(),
                snapshot.getDescription(),
                snapshot.getSnapshotHash(),
                snapshot.getCreatedAt(),
                true,
                "snapshot");
    }

    private StandardSnapshotPayload toPayload(StandardSnapshot snapshot) {
        try {
            String payloadJson = snapshot.getPayloadJson();
            String actualHash = sha256(payloadJson);
            if (!actualHash.equals(snapshot.getSnapshotHash())) {
                throw new BizException(500, "标准快照 payload hash 校验失败: " + snapshot.getId());
            }
            JsonNode payload = objectMapper.readTree(payloadJson);
            return new StandardSnapshotPayload(
                    toInfo(snapshot).withSource("snapshot"),
                    payload,
                    countArray(payload, "fields"),
                    countArray(payload, "enums"),
                    countArray(payload, "rules"));
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException(500, "读取标准快照 payload 失败: " + e.getMessage());
        }
    }

    private int countArray(JsonNode payload, String fieldName) {
        JsonNode node = payload == null ? null : payload.get(fieldName);
        return node != null && node.isArray() ? node.size() : 0;
    }

    private void requireProject(Long projectId) {
        if (projectId == null) {
            throw new BizException(400, "项目 ID 不能为空");
        }
    }

    private String requiredVersion(String value) {
        if (value == null || value.isBlank()) {
            throw new BizException(400, "标准版本号不能为空");
        }
        String version = value.trim();
        if (version.length() > VERSION_MAX_LENGTH) {
            throw new BizException(400, "标准版本号不能超过 100 个字符");
        }
        return version;
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String sha256(String payloadJson) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(payloadJson.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("当前 JDK 不支持 SHA-256", e);
        }
    }
}
