package com.dataspec.standardref.service.impl;

import com.dataspec.common.exception.BizException;
import com.dataspec.common.sanitize.SensitiveDataSanitizer;
import com.dataspec.enumdict.entity.EnumDict;
import com.dataspec.enumdict.repository.EnumDictRepository;
import com.dataspec.field.entity.Field;
import com.dataspec.field.repository.FieldRepository;
import com.dataspec.rule.entity.RuleConfig;
import com.dataspec.rule.repository.RuleConfigRepository;
import com.dataspec.security.context.ProjectAccessGuard;
import com.dataspec.standard.entity.StandardSnapshot;
import com.dataspec.standard.repository.StandardSnapshotRepository;
import com.dataspec.standardref.model.StandardReferenceConfidence;
import com.dataspec.standardref.model.StandardReferenceResolveRequest;
import com.dataspec.standardref.model.StandardReferenceResolveResponse;
import com.dataspec.standardref.model.StandardReferenceResolutionResult;
import com.dataspec.standardref.model.StandardReferenceResolutionStatus;
import com.dataspec.standardref.model.StandardReferenceType;
import com.dataspec.standardref.service.StandardReferenceFormatter;
import com.dataspec.standardref.service.StandardReferenceResolutionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * 标准对象引用解析服务实现。
 *
 * <p>第一版以现有 projectId/objectId/code/version 派生 stableRef，不新增持久化字段。所有解析都先校验
 * projectId，再在当前项目内扫描小团队规模标准资产；跨项目 stableRef 只返回 CROSS_PROJECT，不查询或泄露目标对象。</p>
 */
@Service
@RequiredArgsConstructor
public class StandardReferenceResolutionServiceImpl implements StandardReferenceResolutionService {

    private static final String STATUS_DEPRECATED = "deprecated";
    private static final String STATUS_DISABLED = "disabled";
    private static final int MAX_REF_TEXT_LENGTH = 200;

    private final FieldRepository fieldRepository;
    private final EnumDictRepository enumDictRepository;
    private final RuleConfigRepository ruleConfigRepository;
    private final StandardSnapshotRepository standardSnapshotRepository;

    @Override
    public StandardReferenceResolveResponse resolve(StandardReferenceResolveRequest request) {
        validate(request);
        ProjectAccessGuard.requireProjectAccess(request.projectId());
        List<StandardReferenceResolutionResult> results = new ArrayList<>();
        for (String ref : request.refs()) {
            results.add(resolveOne(request.projectId(), request.refType(), ref));
        }
        return new StandardReferenceResolveResponse(
                StandardReferenceResolveResponse.KIND,
                StandardReferenceResolveResponse.SCHEMA_VERSION,
                request.projectId(),
                results,
                List.of());
    }

    private void validate(StandardReferenceResolveRequest request) {
        if (request == null || request.projectId() == null) {
            throw new BizException("projectId 不能为空");
        }
        if (request.refType() == null) {
            throw new BizException("refType 不能为空");
        }
        if (request.refs() == null || request.refs().isEmpty()) {
            throw new BizException("refs 不能为空");
        }
    }

    private StandardReferenceResolutionResult resolveOne(Long projectId, StandardReferenceType refType, String rawRef) {
        String inputRef = sanitize(rawRef);
        if (rawRef == null || rawRef.isBlank()) {
            return unknown(inputRef, refType, "引用不能为空");
        }
        Optional<StandardReferenceFormatter.ParsedStableReference> parsed = StandardReferenceFormatter.parse(rawRef.trim());
        if (parsed.isPresent()) {
            return resolveStableRef(projectId, refType, inputRef, parsed.get());
        }
        return switch (refType) {
            case FIELD -> resolveFieldText(projectId, inputRef, rawRef);
            case ENUM -> resolveEnumText(projectId, inputRef, rawRef);
            case RULE -> resolveRuleText(projectId, inputRef, rawRef);
            case SNAPSHOT -> resolveSnapshotText(projectId, inputRef, rawRef);
        };
    }

    private StandardReferenceResolutionResult resolveStableRef(
            Long projectId,
            StandardReferenceType requestedType,
            String inputRef,
            StandardReferenceFormatter.ParsedStableReference parsed
    ) {
        if (!Objects.equals(projectId, parsed.projectId())) {
            return result(
                    inputRef,
                    requestedType,
                    StandardReferenceResolutionStatus.CROSS_PROJECT,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    StandardReferenceConfidence.HIGH,
                    List.of(),
                    List.of("stableRef 指向其他项目，已按跨项目引用拒绝解析。"));
        }
        if (requestedType != parsed.type()) {
            return unknown(inputRef, requestedType, "stableRef 类型与请求 refType 不一致。");
        }
        return switch (parsed.type()) {
            case FIELD -> parseLong(parsed.objectKey())
                    .map(id -> resolveFieldById(projectId, inputRef, id))
                    .orElseGet(() -> unknown(inputRef, requestedType, "字段 stableRef 对象 ID 无效。"));
            case ENUM -> parseLong(parsed.objectKey())
                    .map(id -> resolveEnumById(projectId, inputRef, id))
                    .orElseGet(() -> unknown(inputRef, requestedType, "枚举 stableRef 对象 ID 无效。"));
            case RULE -> resolveRuleText(projectId, inputRef, parsed.objectKey());
            case SNAPSHOT -> resolveSnapshotText(projectId, inputRef, parsed.objectKey());
        };
    }

    private StandardReferenceResolutionResult resolveFieldById(Long projectId, String inputRef, Long fieldId) {
        return fieldRepository.findAllByProjectId(projectId).stream()
                .filter(field -> Objects.equals(fieldId, field.getId()))
                .findFirst()
                .map(field -> fieldResult(inputRef, field, null, StandardReferenceConfidence.HIGH))
                .orElseGet(() -> unknown(inputRef, StandardReferenceType.FIELD, "当前项目不存在该字段 stableRef。"));
    }

    private StandardReferenceResolutionResult resolveFieldText(Long projectId, String inputRef, String rawRef) {
        String key = normalize(rawRef);
        List<FieldMatch> matches = new ArrayList<>();
        for (Field field : fieldRepository.findAllByProjectId(projectId)) {
            FieldMatch match = matchField(field, key);
            if (match != null) {
                matches.add(match);
            }
        }
        if (matches.isEmpty()) {
            return unknown(inputRef, StandardReferenceType.FIELD, "当前项目未找到该字段名、显示名或别名。");
        }
        if (matches.size() > 1) {
            return ambiguous(inputRef, StandardReferenceType.FIELD, matches.stream()
                    .map(match -> evidenceLink(StandardReferenceType.FIELD, match.field().getId()))
                    .toList());
        }
        FieldMatch match = matches.getFirst();
        return fieldResult(inputRef, match.field(), match.matchedAlias(), match.confidence());
    }

    private FieldMatch matchField(Field field, String key) {
        if (sameRefText(field.getName(), key)) {
            return new FieldMatch(field, null, StandardReferenceConfidence.HIGH);
        }
        if (sameRefText(field.getDisplayName(), key)) {
            return new FieldMatch(field, sanitize(field.getDisplayName()), StandardReferenceConfidence.HIGH);
        }
        for (String alias : splitCsv(field.getAliases())) {
            if (sameRefText(alias, key)) {
                return new FieldMatch(field, sanitize(alias), StandardReferenceConfidence.HIGH);
            }
        }
        return null;
    }

    private StandardReferenceResolutionResult fieldResult(
            String inputRef,
            Field field,
            String matchedAlias,
            StandardReferenceConfidence confidence
    ) {
        String stableRef = StandardReferenceFormatter.fieldRef(field.getProjectId(), field.getId());
        Replacement replacement = replacementFor(field);
        boolean stale = isStaleField(field);
        String canonicalRef = replacement.ref() != null ? replacement.ref() : stableRef;
        List<String> warnings = new ArrayList<>();
        if (stale) {
            warnings.add(replacement.ref() == null
                    ? "字段已废弃或停用，且未配置可验证替代字段；需要人工确认。"
                    : "字段已废弃或停用，建议使用替代字段 " + replacement.ref() + "。");
        }
        return result(
                inputRef,
                StandardReferenceType.FIELD,
                stale ? StandardReferenceResolutionStatus.STALE : StandardReferenceResolutionStatus.CURRENT,
                stableRef,
                canonicalRef,
                field.getId(),
                sanitize(field.getName()),
                matchedAlias,
                sanitize(normalizeStatus(field.getStatus())),
                replacement.ref(),
                confidence,
                List.of(evidenceLink(StandardReferenceType.FIELD, field.getId())),
                warnings);
    }

    private Replacement replacementFor(Field field) {
        Long replacementFieldId = field.getReplacementFieldId();
        if (replacementFieldId == null || field.getProjectId() == null) {
            return Replacement.none();
        }
        return fieldRepository.findById(replacementFieldId)
                .filter(replacement -> Objects.equals(field.getProjectId(), replacement.getProjectId()))
                .map(replacement -> new Replacement(StandardReferenceFormatter.fieldRef(replacement.getProjectId(), replacement.getId())))
                .orElse(Replacement.none());
    }

    private boolean isStaleField(Field field) {
        String status = normalizeStatus(field.getStatus());
        return STATUS_DEPRECATED.equals(status)
                || STATUS_DISABLED.equals(status)
                || field.getReplacementFieldId() != null;
    }

    private StandardReferenceResolutionResult resolveEnumById(Long projectId, String inputRef, Long enumId) {
        return enumDictRepository.findDictsByProjectId(projectId).stream()
                .filter(dict -> Objects.equals(enumId, dict.getId()))
                .findFirst()
                .map(dict -> enumResult(inputRef, dict, null))
                .orElseGet(() -> unknown(inputRef, StandardReferenceType.ENUM, "当前项目不存在该枚举 stableRef。"));
    }

    private StandardReferenceResolutionResult resolveEnumText(Long projectId, String inputRef, String rawRef) {
        String key = normalize(rawRef);
        List<EnumDict> matches = enumDictRepository.findDictsByProjectId(projectId).stream()
                .filter(dict -> sameRefText(dict.getCode(), key) || sameRefText(dict.getName(), key))
                .toList();
        if (matches.isEmpty()) {
            return unknown(inputRef, StandardReferenceType.ENUM, "当前项目未找到该枚举代码集。");
        }
        if (matches.size() > 1) {
            return ambiguous(inputRef, StandardReferenceType.ENUM, matches.stream()
                    .map(dict -> evidenceLink(StandardReferenceType.ENUM, dict.getId()))
                    .toList());
        }
        return enumResult(inputRef, matches.getFirst(), sameRefText(matches.getFirst().getName(), key) ? sanitize(matches.getFirst().getName()) : null);
    }

    private StandardReferenceResolutionResult enumResult(String inputRef, EnumDict dict, String matchedAlias) {
        String stableRef = StandardReferenceFormatter.enumRef(dict.getProjectId(), dict.getId());
        return result(
                inputRef,
                StandardReferenceType.ENUM,
                StandardReferenceResolutionStatus.CURRENT,
                stableRef,
                stableRef,
                dict.getId(),
                sanitize(dict.getCode()),
                matchedAlias,
                "enabled",
                null,
                StandardReferenceConfidence.HIGH,
                List.of(evidenceLink(StandardReferenceType.ENUM, dict.getId())),
                List.of());
    }

    private StandardReferenceResolutionResult resolveRuleText(Long projectId, String inputRef, String rawRef) {
        String key = normalize(rawRef);
        List<RuleConfig> matches = ruleConfigRepository.findByProjectId(projectId).stream()
                .filter(rule -> sameRefText(rule.getRuleCode(), key) || sameRefText(rule.getRuleName(), key))
                .toList();
        if (matches.isEmpty()) {
            return unknown(inputRef, StandardReferenceType.RULE, "当前项目未找到该规则编码。");
        }
        if (matches.size() > 1) {
            return ambiguous(inputRef, StandardReferenceType.RULE, matches.stream()
                    .map(rule -> "dataspec://rules/" + sanitize(rule.getRuleCode()))
                    .toList());
        }
        RuleConfig rule = matches.getFirst();
        String stableRef = StandardReferenceFormatter.ruleRef(projectId, rule.getRuleCode());
        return result(
                inputRef,
                StandardReferenceType.RULE,
                Boolean.FALSE.equals(rule.getEnabled())
                        ? StandardReferenceResolutionStatus.STALE
                        : StandardReferenceResolutionStatus.CURRENT,
                stableRef,
                stableRef,
                rule.getId(),
                sanitize(rule.getRuleCode()),
                sameRefText(rule.getRuleName(), key) ? sanitize(rule.getRuleName()) : null,
                Boolean.FALSE.equals(rule.getEnabled()) ? "disabled" : "enabled",
                null,
                StandardReferenceConfidence.HIGH,
                List.of("dataspec://rules/" + sanitize(rule.getRuleCode())),
                Boolean.FALSE.equals(rule.getEnabled()) ? List.of("规则已停用，不能作为当前强约束引用。") : List.of());
    }

    private StandardReferenceResolutionResult resolveSnapshotText(Long projectId, String inputRef, String rawRef) {
        String key = normalize(rawRef);
        List<StandardSnapshot> matches = standardSnapshotRepository.findByProjectId(projectId).stream()
                .filter(snapshot -> sameRefText(String.valueOf(snapshot.getId()), key)
                        || sameRefText(snapshot.getVersion(), key)
                        || sameRefText(snapshot.getName(), key))
                .toList();
        if (matches.isEmpty()) {
            return unknown(inputRef, StandardReferenceType.SNAPSHOT, "当前项目未找到该标准快照。");
        }
        if (matches.size() > 1) {
            return ambiguous(inputRef, StandardReferenceType.SNAPSHOT, matches.stream()
                    .map(snapshot -> evidenceLink(StandardReferenceType.SNAPSHOT, snapshot.getId()))
                    .toList());
        }
        StandardSnapshot snapshot = matches.getFirst();
        String stableRef = StandardReferenceFormatter.snapshotRef(projectId, snapshot.getId());
        return result(
                inputRef,
                StandardReferenceType.SNAPSHOT,
                StandardReferenceResolutionStatus.CURRENT,
                stableRef,
                stableRef,
                snapshot.getId(),
                sanitize(snapshot.getVersion()),
                sameRefText(snapshot.getName(), key) ? sanitize(snapshot.getName()) : null,
                "snapshot",
                null,
                StandardReferenceConfidence.HIGH,
                List.of(evidenceLink(StandardReferenceType.SNAPSHOT, snapshot.getId())),
                List.of());
    }

    private StandardReferenceResolutionResult unknown(String inputRef, StandardReferenceType refType, String warning) {
        return result(
                inputRef,
                refType,
                StandardReferenceResolutionStatus.UNKNOWN,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                StandardReferenceConfidence.LOW,
                List.of(),
                List.of(warning));
    }

    private StandardReferenceResolutionResult ambiguous(String inputRef, StandardReferenceType refType, List<String> evidenceLinks) {
        return result(
                inputRef,
                refType,
                StandardReferenceResolutionStatus.AMBIGUOUS,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                StandardReferenceConfidence.LOW,
                evidenceLinks,
                List.of("引用在当前项目内命中多个对象，请改用 stableRef 或更精确名称。"));
    }

    private StandardReferenceResolutionResult result(
            String inputRef,
            StandardReferenceType refType,
            StandardReferenceResolutionStatus status,
            String stableRef,
            String canonicalRef,
            Long objectId,
            String currentName,
            String matchedAlias,
            String lifecycleStatus,
            String replacementRef,
            StandardReferenceConfidence confidence,
            List<String> evidenceLinks,
            List<String> warnings
    ) {
        return new StandardReferenceResolutionResult(
                inputRef,
                refType,
                status,
                stableRef,
                canonicalRef,
                objectId,
                currentName,
                matchedAlias,
                lifecycleStatus,
                replacementRef,
                confidence,
                sanitizeList(evidenceLinks),
                sanitizeList(warnings));
    }

    private String evidenceLink(StandardReferenceType type, Object id) {
        return switch (type) {
            case FIELD -> "dataspec://fields/" + id;
            case ENUM -> "dataspec://enums/" + id;
            case RULE -> "dataspec://rules/" + id;
            case SNAPSHOT -> "dataspec://standard-snapshots/" + id;
        };
    }

    private List<String> sanitizeList(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        Set<String> sanitized = new LinkedHashSet<>();
        for (String value : values) {
            String text = sanitize(value);
            if (text != null && !text.isBlank()) {
                sanitized.add(text);
            }
        }
        return List.copyOf(sanitized);
    }

    private String sanitize(String value) {
        return SensitiveDataSanitizer.redactText(value, MAX_REF_TEXT_LENGTH);
    }

    private boolean sameRefText(String value, String normalizedKey) {
        return normalize(value).equals(normalizedKey);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeStatus(String status) {
        return status == null || status.isBlank() ? "enabled" : status.trim().toLowerCase(Locale.ROOT);
    }

    private List<String> splitCsv(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        List<String> values = new ArrayList<>();
        for (String part : value.split(",")) {
            if (!part.isBlank()) {
                values.add(part.trim());
            }
        }
        return List.copyOf(values);
    }

    private Optional<Long> parseLong(String value) {
        try {
            return Optional.of(Long.valueOf(value));
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
    }

    private record FieldMatch(Field field, String matchedAlias, StandardReferenceConfidence confidence) {
    }

    private record Replacement(String ref) {
        static Replacement none() {
            return new Replacement(null);
        }
    }
}
