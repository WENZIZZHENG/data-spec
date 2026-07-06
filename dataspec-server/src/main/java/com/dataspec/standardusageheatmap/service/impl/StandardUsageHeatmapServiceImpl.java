package com.dataspec.standardusageheatmap.service.impl;

import com.dataspec.aireplay.entity.AiJobRecord;
import com.dataspec.aireplay.repository.AiJobRecordRepository;
import com.dataspec.common.exception.BizException;
import com.dataspec.common.sanitize.SensitiveDataSanitizer;
import com.dataspec.field.entity.Field;
import com.dataspec.field.service.FieldService;
import com.dataspec.fieldconflict.model.FieldConflictField;
import com.dataspec.fieldconflict.model.FieldConflictGroup;
import com.dataspec.fieldconflict.model.FieldConflictReport;
import com.dataspec.fieldconflict.service.FieldConflictService;
import com.dataspec.fieldquality.model.FieldQualityItem;
import com.dataspec.fieldquality.model.FieldQualityReport;
import com.dataspec.fieldquality.service.FieldQualityService;
import com.dataspec.lint.entity.SqlCheckRecord;
import com.dataspec.lint.repository.SqlCheckRecordRepository;
import com.dataspec.reverseimport.entity.FieldSource;
import com.dataspec.reverseimport.repository.FieldSourceRepository;
import com.dataspec.security.context.ProjectAccessGuard;
import com.dataspec.standardusageheatmap.model.StandardUsageHeatmapItem;
import com.dataspec.standardusageheatmap.model.StandardUsageHeatmapReport;
import com.dataspec.standardusageheatmap.model.StandardUsageHeatmapSummary;
import com.dataspec.standardusageheatmap.service.StandardUsageHeatmapService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 基于近期 SQL/AI 记录和字段治理信号实时生成只读使用热区报告。
 */
@Service
@RequiredArgsConstructor
public class StandardUsageHeatmapServiceImpl implements StandardUsageHeatmapService {

    private static final int RECENT_RECORD_LIMIT = 200;
    private static final int HOT_FIELD_THRESHOLD = 70;
    private static final int RISKY_FIELD_THRESHOLD = 70;

    private final FieldService fieldService;
    private final FieldQualityService fieldQualityService;
    private final FieldConflictService fieldConflictService;
    private final FieldSourceRepository fieldSourceRepository;
    private final SqlCheckRecordRepository sqlCheckRecordRepository;
    private final AiJobRecordRepository aiJobRecordRepository;

    @Override
    @Transactional(readOnly = true)
    public StandardUsageHeatmapReport report(Long projectId) {
        if (projectId == null) {
            throw new BizException("项目ID不能为空");
        }
        ProjectAccessGuard.requireProjectAccess(projectId);
        List<Field> fields = fieldService.listByProject(projectId);
        Map<Long, FieldQualityItem> qualityByFieldId = qualityByFieldId(projectId);
        Map<Long, Integer> conflictCountByFieldId = conflictCountByFieldId(projectId, fields);
        Map<Long, List<FieldSource>> sourcesByFieldId = fieldSourceRepository.findSummaryByProjectId(projectId).stream()
                .filter(source -> source.getFieldId() != null)
                .collect(Collectors.groupingBy(FieldSource::getFieldId, LinkedHashMap::new, Collectors.toList()));
        List<SqlCheckRecord> sqlRecords = sqlCheckRecordRepository.findRecentByProjectId(projectId, RECENT_RECORD_LIMIT);
        List<AiJobRecord> aiJobs = aiJobRecordRepository.findRecentSummaryByProjectId(projectId, RECENT_RECORD_LIMIT);

        List<StandardUsageHeatmapItem> items = fields.stream()
                .map(field -> heatmapItem(
                        field,
                        qualityByFieldId.get(field.getId()),
                        conflictCountByFieldId.getOrDefault(field.getId(), 0),
                        sourcesByFieldId.getOrDefault(field.getId(), List.of()),
                        sqlRecords,
                        aiJobs))
                .sorted(Comparator
                        .comparingInt(StandardUsageHeatmapItem::cleanupPriority).reversed()
                        .thenComparing(StandardUsageHeatmapItem::usageScore, Comparator.reverseOrder())
                        .thenComparing(StandardUsageHeatmapItem::name, Comparator.nullsLast(String::compareTo)))
                .toList();
        return new StandardUsageHeatmapReport(projectId, summary(items), items);
    }

    private StandardUsageHeatmapItem heatmapItem(
            Field field,
            FieldQualityItem quality,
            int conflictCount,
            List<FieldSource> sources,
            List<SqlCheckRecord> sqlRecords,
            List<AiJobRecord> aiJobs
    ) {
        UsageHits lintHits = lintHits(field.getName(), sqlRecords);
        UsageHits aiHits = aiHits(field.getName(), aiJobs);
        int sourceEvidenceCount = sources.size();
        int usageScore = usageScore(lintHits.count(), aiHits.count(), sourceEvidenceCount);
        boolean noRecentUsage = lintHits.count() == 0 && aiHits.count() == 0;
        int cleanupPriority = cleanupPriority(field, quality, conflictCount, sourceEvidenceCount, usageScore, noRecentUsage);
        return new StandardUsageHeatmapItem(
                field.getId(),
                field.getName(),
                field.getDisplayName(),
                field.getStatus(),
                sourceKinds(sources),
                quality == null ? null : quality.getScore(),
                quality == null || quality.getLevel() == null ? null : quality.getLevel().name(),
                conflictCount,
                sourceEvidenceCount,
                lintHits.count(),
                aiHits.count(),
                latest(lintHits.lastReferencedAt(), aiHits.lastReferencedAt()),
                usageScore,
                cleanupPriority,
                suggestedNextAction(field, quality, conflictCount, sourceEvidenceCount, usageScore, cleanupPriority, noRecentUsage));
    }

    private Map<Long, FieldQualityItem> qualityByFieldId(Long projectId) {
        FieldQualityReport report = fieldQualityService.report(projectId);
        if (report == null || report.getFields() == null) {
            return Map.of();
        }
        Map<Long, FieldQualityItem> result = new LinkedHashMap<>();
        for (FieldQualityItem item : report.getFields()) {
            if (item.getFieldId() != null) {
                result.put(item.getFieldId(), item);
            }
        }
        return result;
    }

    private Map<Long, Integer> conflictCountByFieldId(Long projectId, List<Field> fields) {
        FieldConflictReport report = fieldConflictService.report(projectId, fields);
        if (report == null || report.getGroups() == null) {
            return Map.of();
        }
        Map<Long, Integer> result = new LinkedHashMap<>();
        for (FieldConflictGroup group : report.getGroups()) {
            if (group.getFields() == null) {
                continue;
            }
            for (FieldConflictField field : group.getFields()) {
                if (field.getFieldId() != null) {
                    result.merge(field.getFieldId(), 1, Integer::sum);
                }
            }
        }
        return result;
    }

    private UsageHits lintHits(String fieldName, List<SqlCheckRecord> records) {
        int count = 0;
        LocalDateTime lastReferencedAt = null;
        for (SqlCheckRecord record : records) {
            if (!matchesAny(fieldName, record.getOriginalSql(), record.getFixedSql(), record.getIssuesJson())) {
                continue;
            }
            count += 1;
            lastReferencedAt = latest(lastReferencedAt, record.getCreatedAt());
        }
        return new UsageHits(count, lastReferencedAt);
    }

    private UsageHits aiHits(String fieldName, List<AiJobRecord> records) {
        int count = 0;
        LocalDateTime lastReferencedAt = null;
        for (AiJobRecord record : records) {
            if (!matchesAny(fieldName, record.getTitle(), record.getInputSummary(), record.getPromptVersion(), record.getStatus())) {
                continue;
            }
            count += 1;
            lastReferencedAt = latest(lastReferencedAt, record.getCreatedAt());
        }
        return new UsageHits(count, lastReferencedAt);
    }

    private boolean matchesAny(String fieldName, String... texts) {
        if (fieldName == null || fieldName.isBlank()) {
            return false;
        }
        Pattern pattern = Pattern.compile("(^|[^A-Za-z0-9_])" + Pattern.quote(fieldName) + "([^A-Za-z0-9_]|$)", Pattern.CASE_INSENSITIVE);
        for (String text : texts) {
            if (text != null && pattern.matcher(text).find()) {
                return true;
            }
        }
        return false;
    }

    private List<String> sourceKinds(List<FieldSource> sources) {
        Set<String> result = new LinkedHashSet<>();
        for (FieldSource source : sources) {
            String kind = source.getSourceType() == null || source.getSourceType().isBlank()
                    ? "unknown"
                    : source.getSourceType().trim().toLowerCase(Locale.ROOT);
            result.add(SensitiveDataSanitizer.redactText(kind, 80));
        }
        return new ArrayList<>(result);
    }

    private int usageScore(int lintHits, int aiJobHits, int sourceEvidenceCount) {
        int score = 0;
        score += Math.min(50, lintHits * 35);
        score += Math.min(40, aiJobHits * 35);
        if (sourceEvidenceCount > 0) {
            score += 10;
        }
        return clamp(score);
    }

    private int cleanupPriority(
            Field field,
            FieldQualityItem quality,
            int conflictCount,
            int sourceEvidenceCount,
            int usageScore,
            boolean noRecentUsage
    ) {
        int priority = 0;
        if (quality == null) {
            priority += 15;
        } else if (quality.getScore() < 65) {
            priority += 35;
        } else if (quality.getScore() < 85) {
            priority += 15;
        }
        priority += Math.min(40, conflictCount * 25);
        boolean deprecatedOrDisabled = isDeprecatedOrDisabled(field);
        if (deprecatedOrDisabled) {
            priority += 30;
            if (noRecentUsage) {
                priority += 30;
            }
        } else if (isDraft(field)) {
            priority += 10;
        }
        if (usageScore == 0) {
            priority += 20;
        } else if (usageScore >= HOT_FIELD_THRESHOLD
                && (conflictCount > 0 || quality != null && quality.getScore() < 65)) {
            priority += 45;
        }
        if (sourceEvidenceCount == 0) {
            priority += 15;
        }
        return clamp(priority);
    }

    private String suggestedNextAction(
            Field field,
            FieldQualityItem quality,
            int conflictCount,
            int sourceEvidenceCount,
            int usageScore,
            int cleanupPriority,
            boolean noRecentUsage
    ) {
        if (usageScore >= HOT_FIELD_THRESHOLD && ((quality != null && quality.getScore() < 65) || conflictCount > 0)) {
            return "高使用且存在质量或冲突风险，优先修复字段质量和冲突。";
        }
        if (isDeprecatedOrDisabled(field) && noRecentUsage) {
            return "字段已废弃或停用且近期未命中，确认迁移完成后归档。";
        }
        if (isDeprecatedOrDisabled(field)) {
            return "字段已废弃或停用但仍有近期引用，先确认调用方迁移状态再处理。";
        }
        if (sourceEvidenceCount == 0 && usageScore == 0) {
            return "近期未命中且缺少来源证据，补充来源或确认是否清理。";
        }
        if (cleanupPriority >= RISKY_FIELD_THRESHOLD) {
            return "治理优先级较高，建议复核质量、冲突和来源证据。";
        }
        if (usageScore >= HOT_FIELD_THRESHOLD) {
            return "字段近期使用较高，保持标准稳定并关注变更影响。";
        }
        return "低优先级观察，后续结合业务代码引用或语义索引复核。";
    }

    private StandardUsageHeatmapSummary summary(List<StandardUsageHeatmapItem> items) {
        int hot = 0;
        int risky = 0;
        int cleanupCandidate = 0;
        int withoutSource = 0;
        int totalPriority = 0;
        for (StandardUsageHeatmapItem item : items) {
            if (item.usageScore() >= HOT_FIELD_THRESHOLD) {
                hot += 1;
            }
            if (item.cleanupPriority() >= RISKY_FIELD_THRESHOLD) {
                risky += 1;
            }
            boolean noRecentUsage = item.lintHits() == 0 && item.aiJobHits() == 0;
            if (item.cleanupPriority() >= 60
                    && noRecentUsage
                    && (item.usageScore() == 0 || isDeprecatedOrDisabledStatus(item.status()))) {
                cleanupCandidate += 1;
            }
            if (item.sourceEvidenceCount() == 0) {
                withoutSource += 1;
            }
            totalPriority += item.cleanupPriority();
        }
        int averagePriority = items.isEmpty() ? 0 : Math.round((float) totalPriority / items.size());
        return new StandardUsageHeatmapSummary(items.size(), hot, risky, cleanupCandidate, withoutSource, averagePriority);
    }

    private boolean isDeprecatedOrDisabled(Field field) {
        return isDeprecatedOrDisabledStatus(normalizeStatus(field));
    }

    private boolean isDeprecatedOrDisabledStatus(String status) {
        String normalized = status == null ? "" : status.trim().toLowerCase(Locale.ROOT);
        return "deprecated".equals(normalized) || "disabled".equals(normalized);
    }

    private boolean isDraft(Field field) {
        return "draft".equals(normalizeStatus(field));
    }

    private String normalizeStatus(Field field) {
        return field.getStatus() == null ? "" : field.getStatus().trim().toLowerCase(Locale.ROOT);
    }

    private LocalDateTime latest(LocalDateTime left, LocalDateTime right) {
        if (left == null) {
            return right;
        }
        if (right == null) {
            return left;
        }
        return left.isAfter(right) ? left : right;
    }

    private int clamp(int value) {
        return Math.max(0, Math.min(100, value));
    }

    private record UsageHits(int count, LocalDateTime lastReferencedAt) {
    }
}
