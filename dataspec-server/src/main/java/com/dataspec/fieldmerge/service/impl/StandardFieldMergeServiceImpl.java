package com.dataspec.fieldmerge.service.impl;

import com.dataspec.changelog.service.StandardChangeLogService;
import com.dataspec.common.exception.BizException;
import com.dataspec.common.sanitize.SensitiveDataSanitizer;
import com.dataspec.field.entity.Field;
import com.dataspec.field.model.FieldGroupingSummaries;
import com.dataspec.field.repository.FieldRepository;
import com.dataspec.fieldmerge.model.StandardFieldMergeApplyReq;
import com.dataspec.fieldmerge.model.StandardFieldMergeChange;
import com.dataspec.fieldmerge.model.StandardFieldMergeFieldSummary;
import com.dataspec.fieldmerge.model.StandardFieldMergeImpact;
import com.dataspec.fieldmerge.model.StandardFieldMergePreview;
import com.dataspec.fieldmerge.model.StandardFieldMergePreviewReq;
import com.dataspec.fieldmerge.model.StandardFieldMergeResult;
import com.dataspec.fieldmerge.model.StandardFieldMergeRisk;
import com.dataspec.fieldmerge.model.StandardFieldMergeRollbackHint;
import com.dataspec.fieldmerge.service.StandardFieldMergeService;
import com.dataspec.reverseimport.entity.FieldSource;
import com.dataspec.reverseimport.repository.FieldSourceRepository;
import com.dataspec.security.context.ProjectAccessGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 标准字段合并服务实现。
 */
@Service
@RequiredArgsConstructor
public class StandardFieldMergeServiceImpl implements StandardFieldMergeService {

    private static final String KIND_PREVIEW = "standard_field_merge_preview";
    private static final String KIND_RESULT = "standard_field_merge_result";
    private static final int SCHEMA_VERSION = 1;
    private static final String STATUS_DEPRECATED = "deprecated";
    private static final int REPLACEMENT_REASON_LIMIT = 500;

    private final FieldRepository fieldRepository;
    private final FieldSourceRepository sourceRepository;
    private final StandardChangeLogService changeLogService;

    @Override
    public StandardFieldMergePreview preview(StandardFieldMergePreviewReq req) {
        MergeContext context = loadContext(req.projectId(), req.targetFieldId(), req.sourceFieldId());
        return buildPreview(context);
    }

    @Override
    @Transactional
    public StandardFieldMergeResult apply(StandardFieldMergeApplyReq req) {
        String reason = sanitizeText(FieldGroupingSummaries.normalizeText(req.reason()));
        if (reason == null) {
            throw new BizException("合并原因不能为空");
        }
        MergeContext context = loadContext(req.projectId(), req.targetFieldId(), req.sourceFieldId());
        StandardFieldMergePreview preview = buildPreview(context);
        List<StandardFieldMergeRisk> blockingRisks = preview.risks().stream()
                .filter(StandardFieldMergeRisk::blocking)
                .toList();
        if (!blockingRisks.isEmpty()) {
            throw new BizException("合并存在阻断风险: " + blockingRisks.getFirst().code());
        }

        String targetBefore = changeLogService.snapshot(context.target());
        String sourceBefore = changeLogService.snapshot(context.source());
        context.source().setStatus(STATUS_DEPRECATED);
        context.source().setReplacementFieldId(context.target().getId());
        context.source().setReplacementReason(replacementReason(reason, context));

        /*
         * 先用条件更新抢占来源字段的 replacement 状态，再更新目标字段。
         * 这样并发合并同一来源字段时，失败请求不会污染其他目标字段的 aliases/tags。
         */
        if (fieldRepository.deprecateSourceForMergeIfReplacementUnset(context.source()) != 1) {
            throw new BizException("来源字段已被合并，请重新生成预览");
        }
        context.target().setAliases(joinCsv(preview.targetAfter().aliases()));
        context.target().setTags(joinCsv(preview.targetAfter().tags()));
        if (fieldRepository.update(context.target()) != 1) {
            throw new BizException("保留字段更新失败，请重新生成预览");
        }
        changeLogService.recordChange(
                context.projectId(),
                StandardChangeLogService.TARGET_FIELD,
                context.target().getId(),
                StandardChangeLogService.ACTION_UPDATE,
                targetBefore,
                changeLogService.snapshot(context.target()));
        changeLogService.recordChange(
                context.projectId(),
                StandardChangeLogService.TARGET_FIELD,
                context.source().getId(),
                StandardChangeLogService.ACTION_UPDATE,
                sourceBefore,
                changeLogService.snapshot(context.source()));

        return new StandardFieldMergeResult(
                KIND_RESULT,
                SCHEMA_VERSION,
                context.projectId(),
                true,
                preview,
                preview.rollbackHints(),
                List.of("查看目标字段和来源字段的变更日志，必要时按 rollbackHints 回退"));
    }

    private MergeContext loadContext(Long projectId, Long targetFieldId, Long sourceFieldId) {
        if (projectId == null) {
            throw new BizException("项目ID不能为空");
        }
        if (targetFieldId == null || sourceFieldId == null) {
            throw new BizException("保留字段和来源字段不能为空");
        }
        if (Objects.equals(targetFieldId, sourceFieldId)) {
            throw new BizException("保留字段和来源字段不能相同");
        }
        ProjectAccessGuard.requireProjectAccess(projectId);
        Field target = fieldRepository.findById(targetFieldId)
                .orElseThrow(() -> new BizException("保留字段不存在: " + targetFieldId));
        Field source = fieldRepository.findById(sourceFieldId)
                .orElseThrow(() -> new BizException("来源字段不存在: " + sourceFieldId));
        if (!Objects.equals(projectId, target.getProjectId())) {
            throw new BizException("保留字段不属于当前项目: " + targetFieldId);
        }
        if (!Objects.equals(projectId, source.getProjectId())) {
            throw new BizException("来源字段不属于当前项目: " + sourceFieldId);
        }
        if (source.getReplacementFieldId() != null) {
            throw new BizException("来源字段已设置替代字段: " + source.getReplacementFieldId());
        }
        List<Field> projectFields = fieldRepository.findAllByProjectId(projectId);
        return new MergeContext(projectId, target, source, projectFields);
    }

    private StandardFieldMergePreview buildPreview(MergeContext context) {
        List<String> targetAliases = splitCsv(context.target().getAliases());
        List<String> sourceAliases = splitCsv(context.source().getAliases());
        List<String> mergedAliases = mergeValues(targetAliases, sourceAliases,
                context.source().getName(), context.source().getDisplayName());
        List<String> mergedTags = mergeValues(splitCsv(context.target().getTags()), splitCsv(context.source().getTags()));
        List<String> sourceSummaries = sourceSummaries(context.source());
        StandardFieldMergeFieldSummary target = summary(context.target(), targetAliases, splitCsv(context.target().getTags()),
                sourceSummaries(context.target()), context.target().getStatus(), context.target().getReplacementFieldId(),
                context.target().getReplacementReason());
        StandardFieldMergeFieldSummary source = summary(context.source(), sourceAliases, splitCsv(context.source().getTags()),
                sourceSummaries, context.source().getStatus(), context.source().getReplacementFieldId(),
                context.source().getReplacementReason());
        StandardFieldMergeFieldSummary targetAfter = summary(context.target(), mergedAliases, mergedTags,
                sourceSummaries(context.target()), context.target().getStatus(), context.target().getReplacementFieldId(),
                context.target().getReplacementReason());
        StandardFieldMergeFieldSummary sourceAfter = summary(context.source(), sourceAliases, splitCsv(context.source().getTags()),
                sourceSummaries, STATUS_DEPRECATED, context.target().getId(),
                "合并后改用 " + context.target().getName());
        List<StandardFieldMergeRisk> risks = risks(context, mergedAliases);

        return new StandardFieldMergePreview(
                KIND_PREVIEW,
                SCHEMA_VERSION,
                context.projectId(),
                context.target().getId(),
                target,
                source,
                targetAfter,
                sourceAfter,
                changes(context, targetAliases, mergedAliases, mergedTags, sourceSummaries),
                risks,
                impacts(context, sourceSummaries),
                rollbackHints(context),
                nextActions(risks));
    }

    private StandardFieldMergeFieldSummary summary(Field field, List<String> aliases, List<String> tags,
                                                   List<String> sourceSummaries, String status,
                                                   Long replacementFieldId, String replacementReason) {
        return new StandardFieldMergeFieldSummary(
                field.getId(),
                sanitizeText(field.getName()),
                sanitizeText(field.getDisplayName()),
                sanitizeText(field.getDataType()),
                field.getNullable(),
                field.getCodeSetId(),
                field.getSensitive(),
                sanitizeText(status),
                replacementFieldId,
                sanitizeText(replacementReason),
                sanitizeList(aliases),
                sanitizeList(tags),
                sanitizeText(field.getExampleValue()),
                formatNotes(field),
                sanitizeList(sourceSummaries));
    }

    private List<StandardFieldMergeChange> changes(MergeContext context, List<String> targetAliases,
                                                   List<String> mergedAliases, List<String> mergedTags,
                                                   List<String> sourceSummaries) {
        List<StandardFieldMergeChange> changes = new ArrayList<>();
        if (!targetAliases.equals(mergedAliases)) {
            changes.add(new StandardFieldMergeChange(
                    "aliases",
                    targetAliases,
                    mergedAliases,
                    "SAFE_MERGE",
                    "将来源字段名、显示名和别名去重合并到保留字段 aliases"));
        }
        List<String> targetTags = splitCsv(context.target().getTags());
        if (!targetTags.equals(mergedTags)) {
            changes.add(new StandardFieldMergeChange(
                    "tags",
                    targetTags,
                    mergedTags,
                    "SAFE_MERGE",
                    "将来源字段 tags 去重合并到保留字段"));
        }
        if (hasText(context.source().getExampleValue())
                && !Objects.equals(context.target().getExampleValue(), context.source().getExampleValue())) {
            changes.add(new StandardFieldMergeChange(
                    "exampleValue",
                    sanitizeText(context.target().getExampleValue()),
                    sanitizeText(context.source().getExampleValue()),
                    "MANUAL_REVIEW",
                    "来源字段示例值仅用于人工审阅，不覆盖目标字段示例"));
        }
        if (!formatNotes(context.source()).isEmpty()) {
            changes.add(new StandardFieldMergeChange(
                    "formatNotes",
                    formatNotes(context.target()),
                    formatNotes(context.source()),
                    "MANUAL_REVIEW",
                    "来源字段格式约束仅提示人工审阅，不覆盖目标字段权威格式"));
        }
        if (!sourceSummaries.isEmpty()) {
            changes.add(new StandardFieldMergeChange(
                    "sourceSummaries",
                    List.of(),
                    sourceSummaries,
                    "MANUAL_REVIEW",
                    "来源记录保留在来源字段上，通过 replacement 关系保持可追溯"));
        }
        changes.add(new StandardFieldMergeChange(
                "status",
                context.source().getStatus(),
                STATUS_DEPRECATED,
                "SOURCE_DEPRECATE",
                "来源字段合并后废弃并指向保留字段"));
        return List.copyOf(changes);
    }

    private List<StandardFieldMergeRisk> risks(MergeContext context, List<String> mergedAliases) {
        List<StandardFieldMergeRisk> risks = new ArrayList<>();
        if (!Objects.equals(context.target().getDataType(), context.source().getDataType())) {
            risks.add(risk("WARNING", "DATA_TYPE_MISMATCH", "字段类型不一致，目标字段 dataType 不会被覆盖", false,
                    "确认业务含义一致后再合并"));
        }
        if (!Objects.equals(context.target().getNullable(), context.source().getNullable())) {
            risks.add(risk("WARNING", "NULLABILITY_MISMATCH", "空值约束不一致，目标字段 nullable 不会被覆盖", false,
                    "保留目标字段空值约束，必要时单独编辑"));
        }
        if (!Objects.equals(context.target().getCodeSetId(), context.source().getCodeSetId())) {
            risks.add(risk("WARNING", "CODE_SET_MISMATCH", "代码集不一致，目标字段 codeSetId 不会被覆盖", false,
                    "先确认枚举字典是否需要统一"));
        }
        if (!Objects.equals(context.target().getSensitive(), context.source().getSensitive())) {
            risks.add(risk("WARNING", "SENSITIVE_MISMATCH", "敏感字段标记不一致，目标字段 sensitive 不会被覆盖", false,
                    "按更严格的数据安全要求复核"));
        }
        if (!formatNotes(context.target()).equals(formatNotes(context.source()))) {
            risks.add(risk("INFO", "FORMAT_MISMATCH", "字段格式约束不一致，需要人工审阅", false,
                    "如需迁移格式约束，请合并后编辑目标字段"));
        }
        risks.addAll(aliasOwnerRisks(context, mergedAliases));
        return List.copyOf(risks);
    }

    private List<StandardFieldMergeRisk> aliasOwnerRisks(MergeContext context, List<String> mergedAliases) {
        Set<String> candidates = normalizedSet(mergedAliases);
        List<StandardFieldMergeRisk> risks = new ArrayList<>();
        for (Field field : context.projectFields()) {
            if (Objects.equals(field.getId(), context.target().getId()) || Objects.equals(field.getId(), context.source().getId())) {
                continue;
            }
            if (candidates.contains(normalizeToken(field.getName()))) {
                risks.add(risk("ERROR", "ALIAS_OWNER_CONFLICT",
                        "待迁移别名与其他字段名冲突: " + safeFieldLabel(field), true,
                        "先处理其他字段命名或别名后再合并"));
                continue;
            }
            for (String alias : splitCsv(field.getAliases())) {
                if (candidates.contains(normalizeToken(alias))) {
                    risks.add(risk("ERROR", "ALIAS_OWNER_CONFLICT",
                            "待迁移别名已属于其他字段: " + sanitizeText(alias) + " -> " + safeFieldLabel(field), true,
                            "先移除或调整冲突别名后再合并"));
                    break;
                }
            }
        }
        return risks;
    }

    private StandardFieldMergeRisk risk(String severity, String code, String message, boolean blocking, String manualAction) {
        return new StandardFieldMergeRisk(severity, code, sanitizeText(message), blocking, sanitizeText(manualAction));
    }

    private List<StandardFieldMergeImpact> impacts(MergeContext context, List<String> sourceSummaries) {
        List<StandardFieldMergeImpact> impacts = new ArrayList<>();
        impacts.add(new StandardFieldMergeImpact(
                "FIELD_LIFECYCLE",
                context.source().getId(),
                "来源字段将废弃",
                1,
                "来源字段 " + context.source().getName() + " 将标记为 deprecated，并指向 " + context.target().getName(),
                Map.of("replacementFieldId", context.target().getId())));
        if (!sourceSummaries.isEmpty()) {
            impacts.add(new StandardFieldMergeImpact(
                    "FIELD_SOURCE",
                    context.source().getId(),
                    "来源记录保持可追溯",
                    sourceSummaries.size(),
                    "来源字段已有来源记录: " + String.join("、", sourceSummaries),
                    Map.of("sourceCount", sourceSummaries.size())));
        }
        impacts.add(new StandardFieldMergeImpact(
                "AI_CONTEXT",
                context.source().getId(),
                "AI Context 使用替代关系",
                1,
                "AI 应优先使用保留字段，并将来源字段视为历史兼容别名",
                Map.of("targetFieldId", context.target().getId(), "sourceFieldId", context.source().getId())));
        return impacts.stream()
                .map(impact -> new StandardFieldMergeImpact(
                        impact.impactType(),
                        impact.sourceId(),
                        sanitizeText(impact.title()),
                        impact.count(),
                        sanitizeText(impact.description()),
                        impact.metadata()))
                .toList();
    }

    private List<StandardFieldMergeRollbackHint> rollbackHints(MergeContext context) {
        return List.of(
                new StandardFieldMergeRollbackHint(
                        "FIELD_UNDO",
                        "undo_target",
                        "查询目标字段最新 update 变更日志后，可回退 aliases/tags 迁移",
                        "/api/fields/" + context.target().getId() + "/undo?logId=<changeLogId>"),
                new StandardFieldMergeRollbackHint(
                        "FIELD_UNDO",
                        "undo_source",
                        "查询来源字段最新 update 变更日志后，可回退 deprecated/replacement 状态",
                        "/api/fields/" + context.source().getId() + "/undo?logId=<changeLogId>"));
    }

    private List<String> nextActions(List<StandardFieldMergeRisk> risks) {
        if (risks.stream().anyMatch(StandardFieldMergeRisk::blocking)) {
            return List.of("先处理阻断风险，再重新生成合并预览");
        }
        return List.of("填写合并原因后确认应用", "应用后查看两个字段的变更日志和 rollbackHints");
    }

    private List<String> sourceSummaries(Field field) {
        return sourceRepository.findByFieldId(field.getId()).stream()
                .filter(source -> Objects.equals(field.getProjectId(), source.getProjectId()))
                .map(this::sourceSummary)
                .filter(Objects::nonNull)
                .distinct()
                .limit(5)
                .toList();
    }

    private String sourceSummary(FieldSource source) {
        List<String> parts = new ArrayList<>();
        if (hasText(source.getSchemaName())) {
            parts.add(source.getSchemaName());
        }
        if (hasText(source.getTableName())) {
            parts.add(source.getTableName());
        }
        if (hasText(source.getColumnName())) {
            parts.add(source.getColumnName());
        }
        if (parts.isEmpty()) {
            return null;
        }
        return sanitizeText(String.join(".", parts));
    }

    private List<String> formatNotes(Field field) {
        List<String> notes = new ArrayList<>();
        addNote(notes, "formatType", field.getFormatType());
        addNote(notes, "formatPattern", field.getFormatPattern());
        addNote(notes, "formatUnit", field.getFormatUnit());
        addNote(notes, "formatPrecision", field.getFormatPrecision());
        addNote(notes, "formatTimezone", field.getFormatTimezone());
        addNote(notes, "formatNullPolicy", field.getFormatNullPolicy());
        addNote(notes, "validExamplesJson", field.getValidExamplesJson());
        addNote(notes, "invalidExamplesJson", field.getInvalidExamplesJson());
        addNote(notes, "formatNotes", field.getFormatNotes());
        return List.copyOf(notes);
    }

    private void addNote(List<String> notes, String label, String value) {
        String text = sanitizeText(FieldGroupingSummaries.normalizeText(value));
        if (text != null) {
            notes.add(label + "=" + text);
        }
    }

    private String replacementReason(String reason, MergeContext context) {
        String value = "合并原因: " + reason
                + "；merged_from=" + sanitizeText(context.source().getName())
                + "；target=" + sanitizeText(context.target().getName());
        return value.length() > REPLACEMENT_REASON_LIMIT
                ? value.substring(0, REPLACEMENT_REASON_LIMIT)
                : value;
    }

    private List<String> mergeValues(List<String> first, List<String> second, String... extra) {
        LinkedHashMap<String, String> values = new LinkedHashMap<>();
        addValues(values, first);
        addValues(values, second);
        if (extra != null) {
            for (String item : extra) {
                addValue(values, item);
            }
        }
        return List.copyOf(values.values());
    }

    private void addValues(Map<String, String> values, List<String> items) {
        if (items == null) {
            return;
        }
        for (String item : items) {
            addValue(values, item);
        }
    }

    private void addValue(Map<String, String> values, String item) {
        String text = sanitizeText(FieldGroupingSummaries.normalizeText(item));
        if (text != null) {
            values.putIfAbsent(normalizeToken(text), text);
        }
    }

    private List<String> splitCsv(String value) {
        String normalized = FieldGroupingSummaries.normalizeText(value);
        if (normalized == null) {
            return List.of();
        }
        LinkedHashMap<String, String> values = new LinkedHashMap<>();
        for (String part : normalized.split("[,，]")) {
            addValues(values, List.of(part));
        }
        return List.copyOf(values.values());
    }

    private Set<String> normalizedSet(List<String> values) {
        Set<String> normalized = new LinkedHashSet<>();
        for (String value : values) {
            normalized.add(normalizeToken(value));
        }
        return normalized;
    }

    private String normalizeToken(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private List<String> sanitizeList(List<String> values) {
        if (values == null) {
            return List.of();
        }
        return values.stream()
                .map(this::sanitizeText)
                .filter(Objects::nonNull)
                .toList();
    }

    private String sanitizeText(String value) {
        return SensitiveDataSanitizer.redactText(value);
    }

    private String joinCsv(List<String> values) {
        return values == null || values.isEmpty() ? null : String.join(",", values);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String safeFieldLabel(Field field) {
        return sanitizeText(field.getName() + "(#" + field.getId() + ")");
    }

    private record MergeContext(Long projectId, Field target, Field source, List<Field> projectFields) {
    }
}
