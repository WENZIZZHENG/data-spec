package com.dataspec.fieldprovenance.service.impl;

import com.dataspec.common.exception.BizException;
import com.dataspec.common.sanitize.SensitiveDataSanitizer;
import com.dataspec.field.entity.Field;
import com.dataspec.field.service.FieldService;
import com.dataspec.fieldprovenance.model.FieldProvenanceConfidenceItem;
import com.dataspec.fieldprovenance.model.FieldProvenanceConfidenceLevel;
import com.dataspec.fieldprovenance.model.FieldProvenanceConfidenceReport;
import com.dataspec.fieldprovenance.model.FieldProvenanceConfidenceSummary;
import com.dataspec.fieldprovenance.service.FieldProvenanceConfidenceService;
import com.dataspec.fieldquality.model.FieldQualityItem;
import com.dataspec.fieldquality.model.FieldQualityReport;
import com.dataspec.fieldquality.service.FieldQualityService;
import com.dataspec.reverseimport.entity.FieldSource;
import com.dataspec.reverseimport.repository.FieldSourceRepository;
import com.dataspec.security.context.ProjectAccessGuard;
import com.dataspec.standardcandidate.entity.StandardCandidate;
import com.dataspec.standardcandidate.repository.StandardCandidateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 实时聚合字段来源、标准候选和字段质量评分，生成只读 AI 置信度摘要。
 */
@Service
@RequiredArgsConstructor
public class FieldProvenanceConfidenceServiceImpl implements FieldProvenanceConfidenceService {

    private static final int MAX_SOURCE_REFS = 8;
    private static final int SOURCE_REF_MAX_LENGTH = 180;
    private static final Set<String> DECIDED_CANDIDATE_STATUSES = Set.of("ACCEPTED", "MERGED");
    private static final Set<String> PENDING_CANDIDATE_STATUSES = Set.of("PENDING", "POSTPONED");

    private final FieldService fieldService;
    private final FieldSourceRepository fieldSourceRepository;
    private final StandardCandidateRepository candidateRepository;
    private final FieldQualityService fieldQualityService;

    @Override
    @Transactional(readOnly = true)
    public FieldProvenanceConfidenceReport report(Long projectId) {
        if (projectId == null) {
            throw new BizException("项目ID不能为空");
        }
        ProjectAccessGuard.requireProjectAccess(projectId);

        List<Field> fields = fieldService.listByProject(projectId);
        Map<Long, List<FieldSource>> sourcesByFieldId = fieldSourceRepository.findByProjectId(projectId).stream()
                .filter(source -> source.getFieldId() != null)
                .collect(Collectors.groupingBy(FieldSource::getFieldId, LinkedHashMap::new, Collectors.toList()));
        List<StandardCandidate> candidates = candidateRepository.findByProjectId(projectId);
        Map<Long, FieldQualityItem> qualityByFieldId = qualityByFieldId(projectId);

        List<FieldProvenanceConfidenceItem> items = fields.stream()
                .map(field -> confidenceItem(field, sourcesByFieldId.getOrDefault(field.getId(), List.of()), candidates, qualityByFieldId.get(field.getId())))
                .sorted(Comparator
                        .comparingInt(FieldProvenanceConfidenceItem::aiConfidence)
                        .thenComparing(FieldProvenanceConfidenceItem::name, Comparator.nullsLast(String::compareTo)))
                .toList();
        return new FieldProvenanceConfidenceReport(projectId, summary(items), items);
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

    private FieldProvenanceConfidenceItem confidenceItem(
            Field field,
            List<FieldSource> sources,
            List<StandardCandidate> candidates,
            FieldQualityItem quality
    ) {
        List<StandardCandidate> relatedCandidates = relatedCandidates(field, candidates);
        int sourceEvidenceCount = sources.size();
        int candidateEvidenceCount = relatedCandidates.size();
        int evidenceCount = sourceEvidenceCount + candidateEvidenceCount;
        List<String> warnings = new ArrayList<>();
        List<String> sourceRefs = sourceRefs(sources, relatedCandidates);

        int score = 50;
        score += statusScore(field, warnings);
        if (sourceEvidenceCount > 0) {
            score += 15 + Math.min(10, (sourceEvidenceCount - 1) * 5);
        } else {
            score -= 5;
            warnings.add("字段缺少来源证据");
        }

        score += candidateScore(relatedCandidates, warnings);
        score += qualityScore(quality, warnings);
        int aiConfidence = clamp(score);
        FieldProvenanceConfidenceLevel level = level(aiConfidence, evidenceCount);
        return new FieldProvenanceConfidenceItem(
                field.getId(),
                field.getName(),
                field.getDisplayName(),
                field.getStatus(),
                primarySourceType(sources, relatedCandidates),
                sourceRefs,
                sourceEvidenceCount,
                candidateEvidenceCount,
                evidenceCount,
                quality == null ? null : quality.getScore(),
                quality == null || quality.getLevel() == null ? null : quality.getLevel().name(),
                aiConfidence,
                level,
                recommendedUse(level),
                List.copyOf(warnings));
    }

    private int statusScore(Field field, List<String> warnings) {
        String status = normalize(field.getStatus());
        if (status == null || "ENABLED".equals(status)) {
            return 15;
        }
        if ("DRAFT".equals(status)) {
            warnings.add("字段状态为 draft，不应作为强制标准直接使用");
            return -10;
        }
        if ("DEPRECATED".equals(status) || "DISABLED".equals(status)) {
            warnings.add("字段状态为 " + status.toLowerCase(Locale.ROOT) + "，不应作为强制标准直接使用");
            return -15;
        }
        warnings.add("字段状态为 " + field.getStatus() + "，需要确认生命周期语义");
        return -5;
    }

    private int candidateScore(List<StandardCandidate> relatedCandidates, List<String> warnings) {
        if (relatedCandidates.isEmpty()) {
            return 0;
        }
        List<StandardCandidate> decided = relatedCandidates.stream()
                .filter(candidate -> DECIDED_CANDIDATE_STATUSES.contains(normalize(candidate.getStatus())))
                .toList();
        List<StandardCandidate> pending = relatedCandidates.stream()
                .filter(candidate -> PENDING_CANDIDATE_STATUSES.contains(normalize(candidate.getStatus())))
                .toList();
        int score = 0;
        if (!decided.isEmpty()) {
            score += 15 + averageConfidence(decided) / 20;
        }
        if (!pending.isEmpty()) {
            score += 5;
            warnings.add("存在未决标准候选，使用前需要处理 Inbox");
        }
        return score;
    }

    private int qualityScore(FieldQualityItem quality, List<String> warnings) {
        if (quality == null) {
            warnings.add("缺少字段质量评分");
            return -5;
        }
        int score = quality.getScore();
        if (score >= 85) {
            return 10;
        }
        if (score < 65) {
            warnings.add("字段质量评分偏低");
            return -20;
        }
        return 0;
    }

    private List<StandardCandidate> relatedCandidates(Field field, List<StandardCandidate> candidates) {
        Map<Long, StandardCandidate> related = new LinkedHashMap<>();
        for (StandardCandidate candidate : candidates) {
            if (candidate.getId() == null || !isRelatedCandidate(field, candidate)) {
                continue;
            }
            related.put(candidate.getId(), candidate);
        }
        return new ArrayList<>(related.values());
    }

    private boolean isRelatedCandidate(Field field, StandardCandidate candidate) {
        String status = normalize(candidate.getStatus());
        if (DECIDED_CANDIDATE_STATUSES.contains(status) && Objects.equals(field.getId(), candidate.getTargetFieldId())) {
            return true;
        }
        if (PENDING_CANDIDATE_STATUSES.contains(status)) {
            return sameText(field.getName(), candidate.getCandidateName());
        }
        return sameText(field.getName(), candidate.getCandidateName()) && DECIDED_CANDIDATE_STATUSES.contains(status);
    }

    private List<String> sourceRefs(List<FieldSource> sources, List<StandardCandidate> candidates) {
        Set<String> refs = new LinkedHashSet<>();
        for (FieldSource source : sources) {
            refs.add(sanitizeRef(fieldSourceRef(source)));
        }
        for (StandardCandidate candidate : candidates) {
            refs.add(sanitizeRef(candidateRef(candidate)));
        }
        return refs.stream().limit(MAX_SOURCE_REFS).toList();
    }

    private String fieldSourceRef(FieldSource source) {
        List<String> parts = new ArrayList<>();
        addIfPresent(parts, source.getSchemaName());
        addIfPresent(parts, source.getTableName());
        addIfPresent(parts, source.getColumnName());
        String location = parts.isEmpty() ? "fieldSource#" + nullToEmpty(source.getId()) : String.join(".", parts);
        return nullToDefault(source.getSourceType(), "source") + ":" + location;
    }

    private String candidateRef(StandardCandidate candidate) {
        List<String> parts = new ArrayList<>();
        addIfPresent(parts, candidate.getStatus());
        addIfPresent(parts, candidate.getSourceType());
        addIfPresent(parts, candidate.getSourceRef());
        return "candidate:" + (parts.isEmpty() ? "candidate#" + nullToEmpty(candidate.getId()) : String.join(":", parts));
    }

    private String sanitizeRef(String ref) {
        return SensitiveDataSanitizer.redactText(ref, SOURCE_REF_MAX_LENGTH);
    }

    private String primarySourceType(List<FieldSource> sources, List<StandardCandidate> candidates) {
        if (!sources.isEmpty()) {
            return sanitizeRef(normalizeOrOriginal(sources.getFirst().getSourceType(), "source"));
        }
        if (!candidates.isEmpty()) {
            return sanitizeRef("candidate:" + normalizeOrOriginal(candidates.getFirst().getSourceType(), "unknown"));
        }
        return "manual";
    }

    private FieldProvenanceConfidenceLevel level(int aiConfidence, int evidenceCount) {
        // 没有来源或候选证据时，质量分只能说明字段元数据完整，不能证明该字段可作为强标准。
        if (evidenceCount == 0) {
            return FieldProvenanceConfidenceLevel.UNKNOWN;
        }
        if (aiConfidence >= 85) {
            return FieldProvenanceConfidenceLevel.VERIFIED;
        }
        if (aiConfidence >= 65) {
            return FieldProvenanceConfidenceLevel.REVIEW;
        }
        return FieldProvenanceConfidenceLevel.LOW;
    }

    private String recommendedUse(FieldProvenanceConfidenceLevel level) {
        return switch (level) {
            case VERIFIED -> "可作为 AI 首选标准字段，生成 SQL 或数据模型时优先采用。";
            case REVIEW -> "可用于 AI 推荐，但生成前建议复核来源证据与字段质量。";
            case LOW -> "仅作为候选参考，避免直接生成生产 SQL 或强约束。";
            case UNKNOWN -> "缺少可信来源证据，需人工确认后再纳入强制标准。";
        };
    }

    private FieldProvenanceConfidenceSummary summary(List<FieldProvenanceConfidenceItem> items) {
        int verified = 0;
        int review = 0;
        int low = 0;
        int unknown = 0;
        int withSource = 0;
        int withCandidate = 0;
        int withWarnings = 0;
        for (FieldProvenanceConfidenceItem item : items) {
            if (item.confidenceLevel() == FieldProvenanceConfidenceLevel.VERIFIED) {
                verified += 1;
            } else if (item.confidenceLevel() == FieldProvenanceConfidenceLevel.REVIEW) {
                review += 1;
            } else if (item.confidenceLevel() == FieldProvenanceConfidenceLevel.LOW) {
                low += 1;
            } else {
                unknown += 1;
            }
            if (item.sourceEvidenceCount() > 0) {
                withSource += 1;
            }
            if (item.candidateEvidenceCount() > 0) {
                withCandidate += 1;
            }
            if (!item.warnings().isEmpty()) {
                withWarnings += 1;
            }
        }
        return new FieldProvenanceConfidenceSummary(
                items.size(),
                verified,
                review,
                low,
                unknown,
                withSource,
                withCandidate,
                withWarnings);
    }

    private int averageConfidence(List<StandardCandidate> candidates) {
        int total = 0;
        int count = 0;
        for (StandardCandidate candidate : candidates) {
            total += candidate.getConfidence() == null ? 50 : Math.max(0, Math.min(candidate.getConfidence(), 100));
            count += 1;
        }
        return count == 0 ? 50 : Math.round((float) total / count);
    }

    private int clamp(int score) {
        return Math.max(0, Math.min(score, 100));
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

    private String normalizeOrOriginal(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim().toLowerCase(Locale.ROOT);
    }

    private void addIfPresent(List<String> values, String value) {
        if (value != null && !value.isBlank()) {
            values.add(value.trim());
        }
    }

    private String nullToDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private String nullToEmpty(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
