package com.dataspec.standardevidence.service.impl;

import com.dataspec.aireplay.entity.AiJobRecord;
import com.dataspec.aireplay.repository.AiJobRecordRepository;
import com.dataspec.changelog.entity.StandardChangeLog;
import com.dataspec.changelog.repository.StandardChangeLogRepository;
import com.dataspec.common.exception.BizException;
import com.dataspec.common.sanitize.SensitiveDataSanitizer;
import com.dataspec.field.entity.Field;
import com.dataspec.field.service.FieldService;
import com.dataspec.fieldprovenance.model.FieldProvenanceConfidenceItem;
import com.dataspec.fieldprovenance.model.FieldProvenanceConfidenceLevel;
import com.dataspec.fieldprovenance.model.FieldProvenanceConfidenceReport;
import com.dataspec.fieldprovenance.service.FieldProvenanceConfidenceService;
import com.dataspec.reverseimport.entity.FieldSource;
import com.dataspec.reverseimport.repository.FieldSourceRepository;
import com.dataspec.security.context.ProjectAccessGuard;
import com.dataspec.standardcandidate.entity.StandardCandidate;
import com.dataspec.standardcandidate.repository.StandardCandidateRepository;
import com.dataspec.standardevidence.model.StandardEvidenceItem;
import com.dataspec.standardevidence.model.StandardEvidenceReport;
import com.dataspec.standardevidence.model.StandardEvidenceSubject;
import com.dataspec.standardevidence.model.StandardEvidenceSummary;
import com.dataspec.standardevidence.service.StandardEvidenceService;
import com.dataspec.standardusageheatmap.model.StandardUsageHeatmapItem;
import com.dataspec.standardusageheatmap.model.StandardUsageHeatmapReport;
import com.dataspec.standardusageheatmap.service.StandardUsageHeatmapService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 聚合字段来源、可信度、使用、候选、变更、SQL 和 AI 作业摘要，生成只读跨来源证据视图。
 */
@Service
@RequiredArgsConstructor
public class StandardEvidenceServiceImpl implements StandardEvidenceService {

    private static final String SUBJECT_TYPE_FIELD = "FIELD";
    private static final String CHANGE_LOG_TARGET_FIELD = "field";
    private static final int RECENT_RECORD_LIMIT = 200;
    private static final int CHANGE_LOG_LIMIT = 20;
    private static final Set<String> DECIDED_CANDIDATE_STATUSES = Set.of("ACCEPTED", "MERGED");
    private static final Set<String> ACTIVE_CANDIDATE_STATUSES = Set.of("PENDING", "POSTPONED");

    private final FieldService fieldService;
    private final FieldSourceRepository fieldSourceRepository;
    private final FieldProvenanceConfidenceService confidenceService;
    private final StandardUsageHeatmapService usageHeatmapService;
    private final StandardCandidateRepository candidateRepository;
    private final StandardChangeLogRepository changeLogRepository;
    private final AiJobRecordRepository aiJobRecordRepository;

    @Override
    @Transactional(readOnly = true)
    public StandardEvidenceReport report(Long projectId, String subjectType, Long subjectId) {
        if (projectId == null) {
            throw new BizException("项目ID不能为空");
        }
        if (subjectId == null) {
            throw new BizException("证据目标ID不能为空");
        }
        if (!SUBJECT_TYPE_FIELD.equals(normalize(subjectType))) {
            throw new BizException("跨来源证据视图第一版仅支持 FIELD");
        }
        ProjectAccessGuard.requireProjectAccess(projectId);

        Field field = fieldService.getById(subjectId);
        if (field == null || !Objects.equals(projectId, field.getProjectId())) {
            // 字段存在性和归属使用同一个错误，避免把其他项目字段 ID 泄漏给调用方。
            throw new BizException(404, "标准字段不存在或不属于当前项目");
        }

        List<StandardEvidenceItem> items = new ArrayList<>();
        List<String> coverageNotes = new ArrayList<>();

        List<FieldSource> sources = fieldSources(projectId, subjectId);
        addFieldSources(items, sources);

        FieldProvenanceConfidenceItem confidence = confidenceItem(projectId, subjectId);
        addConfidence(items, confidence);

        StandardUsageHeatmapItem usage = usageItem(projectId, subjectId);
        addUsage(items, usage);

        List<StandardCandidate> candidates = relatedCandidates(projectId, field);
        addCandidates(items, candidates);

        List<StandardChangeLog> changeLogs = changeLogRepository.findSummaryByTarget(
                projectId,
                CHANGE_LOG_TARGET_FIELD,
                subjectId,
                CHANGE_LOG_LIMIT);
        addChangeLogs(items, changeLogs);

        UsageHits lintHits = lintHits(usage);
        addLintHits(items, lintHits);

        UsageHits aiHits = aiHits(field.getName(), aiJobRecordRepository.findRecentSummaryByProjectId(projectId, RECENT_RECORD_LIMIT));
        addAiHits(items, aiHits);

        List<StandardEvidenceItem> sortedItems = items.stream()
                .sorted(Comparator
                        .comparing(StandardEvidenceItem::occurredAt, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(StandardEvidenceItem::evidenceType))
                .toList();
        addCoverageNotes(coverageNotes, sources, confidence, usage, candidates, changeLogs, lintHits, aiHits);

        StandardEvidenceSummary summary = summary(sortedItems, confidence, usage, lintHits, aiHits, candidates, changeLogs, coverageNotes);
        String aiEvidenceSummary = aiEvidenceSummary(field, summary, sortedItems, coverageNotes);
        return new StandardEvidenceReport(
                projectId,
                subject(field),
                summary,
                sortedItems,
                aiEvidenceSummary,
                List.copyOf(coverageNotes));
    }

    private StandardEvidenceSubject subject(Field field) {
        return new StandardEvidenceSubject(
                SUBJECT_TYPE_FIELD,
                field.getId(),
                safeText(field.getName(), 120),
                safeText(field.getDisplayName(), 120),
                safeText(field.getDataType(), 80),
                safeText(field.getStatus(), 60));
    }

    private List<FieldSource> fieldSources(Long projectId, Long fieldId) {
        return fieldSourceRepository.findSummaryByProjectId(projectId).stream()
                .filter(source -> Objects.equals(fieldId, source.getFieldId()))
                .toList();
    }

    private void addFieldSources(List<StandardEvidenceItem> items, List<FieldSource> sources) {
        for (FieldSource source : sources) {
            String sourceType = safeText(valueOrDefault(source.getSourceType(), "unknown"), 80);
            items.add(new StandardEvidenceItem(
                    "FIELD_SOURCE",
                    "fieldSource:" + source.getId(),
                    "字段来源",
                    safeText("字段存在 " + sourceType + " 来源记录。", 180),
                    sourceType,
                    null,
                    null,
                    source.getCreatedAt(),
                    refs("fieldSource.id=" + source.getId(), "fieldSource.type=" + sourceType)));
        }
    }

    private FieldProvenanceConfidenceItem confidenceItem(Long projectId, Long fieldId) {
        FieldProvenanceConfidenceReport report = confidenceService.report(projectId);
        if (report == null || report.fields() == null) {
            return null;
        }
        return report.fields().stream()
                .filter(item -> Objects.equals(fieldId, item.fieldId()))
                .findFirst()
                .orElse(null);
    }

    private void addConfidence(List<StandardEvidenceItem> items, FieldProvenanceConfidenceItem confidence) {
        if (confidence == null) {
            return;
        }
        String level = confidence.confidenceLevel() == null ? "UNKNOWN" : confidence.confidenceLevel().name();
        List<String> warningRefs = confidence.warnings() == null
                ? List.of()
                : confidence.warnings().stream()
                        .map(warning -> "warning=" + safeText(warning, 120))
                        .toList();
        List<String> sourceRefs = confidence.sourceRefs() == null
                ? List.of()
                : confidence.sourceRefs().stream()
                        .map(ref -> "sourceRef=" + safeText(ref, 160))
                        .toList();
        List<String> refs = new ArrayList<>();
        refs.add("confidence.level=" + level);
        refs.add("confidence.score=" + confidence.aiConfidence());
        refs.addAll(sourceRefs);
        refs.addAll(warningRefs);
        items.add(new StandardEvidenceItem(
                "PROVENANCE_CONFIDENCE",
                "field:" + confidence.fieldId(),
                "来源可信度",
                safeText("来源可信度为 " + level + "，AI 置信度 " + confidence.aiConfidence()
                        + "，建议：" + valueOrDefault(confidence.recommendedUse(), "人工复核。"), 260),
                safeText(confidence.primarySourceType(), 80),
                level,
                confidence.aiConfidence(),
                null,
                refs));
    }

    private StandardUsageHeatmapItem usageItem(Long projectId, Long fieldId) {
        StandardUsageHeatmapReport report = usageHeatmapService.report(projectId);
        if (report == null || report.items() == null) {
            return null;
        }
        return report.items().stream()
                .filter(item -> Objects.equals(fieldId, item.fieldId()))
                .findFirst()
                .orElse(null);
    }

    private void addUsage(List<StandardEvidenceItem> items, StandardUsageHeatmapItem usage) {
        if (usage == null) {
            return;
        }
        items.add(new StandardEvidenceItem(
                "USAGE_HEATMAP",
                "fieldUsage:" + usage.fieldId(),
                "使用热区",
                safeText("使用热度 " + usage.usageScore() + "，治理优先级 " + usage.cleanupPriority()
                        + "，SQL 检查命中 " + usage.lintHits() + " 次，AI 作业命中 " + usage.aiJobHits() + " 次。", 220),
                safeText(String.join(",", usage.sourceKinds() == null ? List.of() : usage.sourceKinds()), 120),
                safeText(usage.qualityLevel(), 60),
                usage.qualityScore(),
                usage.lastReferencedAt(),
                refs(
                        "usage.score=" + usage.usageScore(),
                        "usage.cleanupPriority=" + usage.cleanupPriority(),
                        "usage.suggestedNextAction=" + safeText(usage.suggestedNextAction(), 120))));
    }

    private List<StandardCandidate> relatedCandidates(Long projectId, Field field) {
        return candidateRepository.findSummaryByProjectId(projectId).stream()
                .filter(candidate -> isRelatedCandidate(field, candidate))
                .limit(8)
                .toList();
    }

    private boolean isRelatedCandidate(Field field, StandardCandidate candidate) {
        String status = normalize(candidate.getStatus());
        if (DECIDED_CANDIDATE_STATUSES.contains(status) && Objects.equals(field.getId(), candidate.getTargetFieldId())) {
            return true;
        }
        if (ACTIVE_CANDIDATE_STATUSES.contains(status)) {
            return sameText(field.getName(), candidate.getCandidateName());
        }
        return DECIDED_CANDIDATE_STATUSES.contains(status) && sameText(field.getName(), candidate.getCandidateName());
    }

    private void addCandidates(List<StandardEvidenceItem> items, List<StandardCandidate> candidates) {
        for (StandardCandidate candidate : candidates) {
            String status = safeText(valueOrDefault(candidate.getStatus(), "UNKNOWN"), 60);
            String sourceType = safeText(candidate.getSourceType(), 80);
            String reason = safeText(valueOrDefault(candidate.getDecisionReason(), "未记录决策理由"), 160);
            LocalDateTime occurredAt = firstPresent(candidate.getDecidedAt(), candidate.getUpdatedAt(), candidate.getCreatedAt());
            items.add(new StandardEvidenceItem(
                    "CANDIDATE_DECISION",
                    "candidate:" + candidate.getId(),
                    "候选决策",
                    safeText("候选 " + safeText(candidate.getCandidateName(), 120) + " 状态为 " + status
                            + "，置信度 " + nullToUnknown(candidate.getConfidence()) + "，决策理由：" + reason, 260),
                    sourceType,
                    status,
                    candidate.getConfidence(),
                    occurredAt,
                    refs(
                            "candidate.name=" + safeText(candidate.getCandidateName(), 120),
                            "candidate.sourceType=" + sourceType,
                            "candidate.sourceRef=" + safeText(candidate.getSourceRef(), 160))));
        }
    }

    private void addChangeLogs(List<StandardEvidenceItem> items, List<StandardChangeLog> changeLogs) {
        for (StandardChangeLog log : changeLogs) {
            String action = safeText(valueOrDefault(log.getAction(), "unknown"), 60);
            String operator = safeText(valueOrDefault(log.getOperatorName(), "unknown"), 80);
            items.add(new StandardEvidenceItem(
                    "CHANGE_LOG",
                    "changeLog:" + log.getId(),
                    "标准变更",
                    safeText("标准字段发生 " + action + " 变更，操作者：" + operator + "。", 200),
                    "change-log",
                    action,
                    null,
                    log.getChangedAt(),
                    refs("changeLog.action=" + action, "changeLog.operator=" + operator)));
        }
    }

    private UsageHits lintHits(StandardUsageHeatmapItem usage) {
        if (usage == null) {
            return new UsageHits(0, 0, null, List.of());
        }
        // SQL 检查命中沿用使用热区已经计算出的安全计数，避免本视图再装载 SQL 原文或 raw issue JSON。
        return new UsageHits(usage.lintHits(), 0, usage.lastReferencedAt(), List.of());
    }

    private void addLintHits(List<StandardEvidenceItem> items, UsageHits hits) {
        if (hits.count() <= 0) {
            return;
        }
        items.add(new StandardEvidenceItem(
                "SQL_LINT_HIT",
                "sqlLint:recent",
                "近期 SQL 检查命中",
                safeText("近期 SQL 检查命中 " + hits.count() + " 次。", 180),
                "sql-check",
                "SUMMARY",
                null,
                hits.lastReferencedAt(),
                refs("sqlLint.hitCount=" + hits.count())));
    }

    private UsageHits aiHits(String fieldName, List<AiJobRecord> records) {
        int count = 0;
        LocalDateTime lastReferencedAt = null;
        Set<String> jobTypes = new LinkedHashSet<>();
        for (AiJobRecord record : records) {
            if (!matchesAny(fieldName, record.getTitle(), record.getInputSummary(), record.getPromptVersion(), record.getStatus())) {
                continue;
            }
            count += 1;
            lastReferencedAt = latest(lastReferencedAt, record.getCreatedAt());
            if (record.getJobType() != null && !record.getJobType().isBlank()) {
                jobTypes.add(safeText(record.getJobType(), 60));
            }
        }
        return new UsageHits(count, 0, lastReferencedAt, new ArrayList<>(jobTypes));
    }

    private void addAiHits(List<StandardEvidenceItem> items, UsageHits hits) {
        if (hits.count() <= 0) {
            return;
        }
        String jobTypes = hits.refs().isEmpty() ? "unknown" : String.join(",", hits.refs());
        items.add(new StandardEvidenceItem(
                "AI_JOB_USAGE",
                "aiJob:recent",
                "近期 AI 作业命中",
                safeText("近期 AI 作业摘要命中 " + hits.count() + " 次，作业类型：" + jobTypes + "。", 180),
                "ai-job",
                "SUMMARY",
                null,
                hits.lastReferencedAt(),
                refs("aiJob.hitCount=" + hits.count(), "aiJob.types=" + jobTypes)));
    }

    private void addCoverageNotes(
            List<String> notes,
            List<FieldSource> sources,
            FieldProvenanceConfidenceItem confidence,
            StandardUsageHeatmapItem usage,
            List<StandardCandidate> candidates,
            List<StandardChangeLog> changeLogs,
            UsageHits lintHits,
            UsageHits aiHits
    ) {
        if (sources.isEmpty()) {
            notes.add("缺少字段来源证据");
        }
        if (confidence == null || confidence.confidenceLevel() == FieldProvenanceConfidenceLevel.UNKNOWN) {
            notes.add("来源可信度不足，需要人工复核");
        }
        if (usage == null) {
            notes.add("缺少标准使用热区证据");
        }
        if (candidates.isEmpty()) {
            notes.add("缺少候选决策证据");
        }
        if (changeLogs.isEmpty()) {
            notes.add("缺少变更日志证据");
        }
        if (lintHits.count() == 0) {
            notes.add("缺少近期 SQL 检查命中");
        }
        if (aiHits.count() == 0) {
            notes.add("缺少近期 AI 作业命中");
        }
        notes.add("SQL 检查和 AI 作业命中基于字段名近似匹配，不代表完整血缘。");
    }

    private StandardEvidenceSummary summary(
            List<StandardEvidenceItem> items,
            FieldProvenanceConfidenceItem confidence,
            StandardUsageHeatmapItem usage,
            UsageHits lintHits,
            UsageHits aiHits,
            List<StandardCandidate> candidates,
            List<StandardChangeLog> changeLogs,
            List<String> coverageNotes
    ) {
        String confidenceLevel = confidence == null || confidence.confidenceLevel() == null
                ? "UNKNOWN"
                : confidence.confidenceLevel().name();
        int aiConfidence = confidence == null ? 0 : confidence.aiConfidence();
        int usageScore = usage == null ? 0 : usage.usageScore();
        LocalDateTime lastEvidenceAt = null;
        for (StandardEvidenceItem item : items) {
            lastEvidenceAt = latest(lastEvidenceAt, item.occurredAt());
        }
        boolean reviewRequired = !"VERIFIED".equals(confidenceLevel)
                || usage != null && usage.cleanupPriority() >= 70
                || coverageNotes.stream().anyMatch(note -> note.startsWith("缺少"));
        return new StandardEvidenceSummary(
                items.size(),
                confidenceLevel,
                aiConfidence,
                usageScore,
                lintHits.count(),
                aiHits.count(),
                candidates.size(),
                changeLogs.size(),
                lastEvidenceAt,
                reviewRequired);
    }

    private String aiEvidenceSummary(
            Field field,
            StandardEvidenceSummary summary,
            List<StandardEvidenceItem> items,
            List<String> coverageNotes
    ) {
        List<String> parts = new ArrayList<>();
        parts.add("字段 " + safeText(field.getName(), 120) + " 当前可信度 " + summary.confidenceLevel()
                + "（AI 置信度 " + summary.aiConfidence() + "）");
        List<String> sourceTypes = primarySourceTypes(items);
        if (!sourceTypes.isEmpty()) {
            parts.add("主要来源 " + String.join("、", sourceTypes));
        }
        parts.add("使用热度 " + summary.usageScore());
        parts.add("SQL 检查命中 " + summary.lintHitCount() + " 次");
        parts.add("AI 作业命中 " + summary.aiJobHitCount() + " 次");
        parts.add("候选决策 " + summary.candidateDecisionCount() + " 条");
        parts.add("变更日志 " + summary.changeLogCount() + " 条");
        List<String> decisionSummary = decisionSummary(items);
        if (!decisionSummary.isEmpty()) {
            parts.add("候选/变更摘要：" + String.join("；", decisionSummary));
        }
        if (summary.reviewRequired()) {
            parts.add("证据不足或存在治理风险，建议人工复核");
        }
        List<String> missing = coverageNotes.stream()
                .filter(note -> note.startsWith("缺少"))
                .limit(4)
                .toList();
        if (!missing.isEmpty()) {
            parts.add("覆盖缺口：" + String.join("、", missing));
        }
        return safeText(String.join("；", parts) + "。", 600);
    }

    private List<String> primarySourceTypes(List<StandardEvidenceItem> items) {
        Set<String> sourceTypes = new LinkedHashSet<>();
        for (StandardEvidenceItem item : items) {
            if (item.sourceType() != null
                    && !item.sourceType().isBlank()
                    && !"sql-check".equals(item.sourceType())
                    && !"ai-job".equals(item.sourceType())
                    && !"change-log".equals(item.sourceType())) {
                sourceTypes.add(safeText(item.sourceType(), 80));
            }
        }
        return sourceTypes.stream().limit(4).toList();
    }

    private List<String> decisionSummary(List<StandardEvidenceItem> items) {
        List<String> result = new ArrayList<>();
        for (StandardEvidenceItem item : items) {
            if ("CANDIDATE_DECISION".equals(item.evidenceType()) || "CHANGE_LOG".equals(item.evidenceType())) {
                result.add(safeText(item.title() + " " + valueOrDefault(item.status(), "SUMMARY"), 120));
            }
        }
        return result.stream().limit(4).toList();
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

    private boolean sameText(String left, String right) {
        return normalize(left) != null && normalize(left).equals(normalize(right));
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private String safeText(String value) {
        return safeText(value, 240);
    }

    private String safeText(String value, int maxLength) {
        String sanitized = SensitiveDataSanitizer.redactText(value, maxLength);
        if (sanitized == null) {
            return null;
        }
        // 证据视图会被复制给 AI，敏感字段名标签本身也容易形成误导上下文，因此统一替换。
        return sanitized.replaceAll(
                "(?i)(access[_-]?token|refresh[_-]?token|plain[_-]?token|token[_-]?hash|client[_-]?secret|api[_-]?key|jdbc[_-]?url|connection[_-]?string|authorization|password|passwd|pwd|secret|\\btoken\\b|\\bdsn\\b|\\bjdbc\\b)",
                "凭据");
    }

    private List<String> refs(String... values) {
        List<String> result = new ArrayList<>();
        if (values == null) {
            return result;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                result.add(safeText(value, 180));
            }
        }
        return result;
    }

    private String valueOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String nullToUnknown(Integer value) {
        return value == null ? "UNKNOWN" : String.valueOf(value);
    }

    private int nullToZero(Integer value) {
        return value == null ? 0 : value;
    }

    private LocalDateTime firstPresent(LocalDateTime... values) {
        for (LocalDateTime value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
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

    private record UsageHits(int count, int issueCount, LocalDateTime lastReferencedAt, List<String> refs) {
    }
}
