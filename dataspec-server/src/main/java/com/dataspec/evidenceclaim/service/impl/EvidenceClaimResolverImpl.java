package com.dataspec.evidenceclaim.service.impl;

import com.dataspec.aibatch.repository.AiBatchRunRepository;
import com.dataspec.aireplay.repository.AiJobRecordRepository;
import com.dataspec.aitaskrun.repository.AiTaskRunRepository;
import com.dataspec.common.sanitize.SensitiveDataSanitizer;
import com.dataspec.evidence.model.EvidenceSourceType;
import com.dataspec.evidenceclaim.model.EvidenceClaimResolution;
import com.dataspec.evidenceclaim.model.EvidenceClaimResolutionStatus;
import com.dataspec.evidenceclaim.service.EvidenceClaimResolver;
import com.dataspec.lint.repository.SqlCheckRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 基于现有持久化来源的 Evidence claim resolver。
 *
 * <p>实现只接受固定 scheme、allowlist source type 和正整数 ID。跨项目命中仅返回
 * CROSS_PROJECT，不返回来源项目、标题、状态或 payload。</p>
 */
@Service
@RequiredArgsConstructor
public class EvidenceClaimResolverImpl implements EvidenceClaimResolver {

    private static final int MAX_REF_LENGTH = 300;
    private static final Pattern EVIDENCE_REF_PATTERN = Pattern.compile(
            "^dataspec://evidence/(sql-check|ai-job|ai-batch-run|ai-task-run)/([1-9][0-9]*)$");

    private final SqlCheckRecordRepository sqlCheckRecordRepository;
    private final AiJobRecordRepository aiJobRecordRepository;
    private final AiBatchRunRepository aiBatchRunRepository;
    private final AiTaskRunRepository aiTaskRunRepository;

    @Override
    public EvidenceClaimResolution resolve(Long projectId, String evidenceRef) {
        String inputRef = SensitiveDataSanitizer.redactText(evidenceRef, MAX_REF_LENGTH);
        if (projectId == null || evidenceRef == null) {
            return result(inputRef, null, EvidenceClaimResolutionStatus.UNVERIFIABLE, null, null, null);
        }
        Matcher matcher = EVIDENCE_REF_PATTERN.matcher(evidenceRef.trim().toLowerCase(Locale.ROOT));
        if (!matcher.matches()) {
            return result(inputRef, null, EvidenceClaimResolutionStatus.UNVERIFIABLE, null, null, null);
        }

        EvidenceSourceType sourceType = sourceType(matcher.group(1));
        Long sourceId = parsePositiveLong(matcher.group(2)).orElse(null);
        if (sourceType == null || sourceId == null) {
            return result(inputRef, null, EvidenceClaimResolutionStatus.UNVERIFIABLE, sourceType, sourceId, null);
        }

        SourceLookup source = findSource(sourceType, sourceId);
        if (!source.exists()) {
            return result(inputRef, null, EvidenceClaimResolutionStatus.MISSING, sourceType, sourceId, null);
        }
        Long ownerProjectId = source.projectId();
        if (ownerProjectId == null) {
            return result(inputRef, null, EvidenceClaimResolutionStatus.UNVERIFIABLE, sourceType, sourceId, null);
        }
        if (!Objects.equals(projectId, ownerProjectId)) {
            return result(inputRef, null, EvidenceClaimResolutionStatus.CROSS_PROJECT, sourceType, sourceId, null);
        }
        return result(
                inputRef,
                canonicalRef(sourceType, sourceId),
                EvidenceClaimResolutionStatus.VERIFIED,
                sourceType,
                sourceId,
                projectId);
    }

    @Override
    public String canonicalRef(EvidenceSourceType sourceType, Long sourceId) {
        if (sourceType == null || sourceId == null || sourceId <= 0) {
            return null;
        }
        String path = switch (sourceType) {
            case SQL_CHECK -> "sql-check";
            case AI_JOB -> "ai-job";
            case AI_BATCH_RUN -> "ai-batch-run";
            case AI_TASK_RUN -> "ai-task-run";
            case COVERAGE_REPORT -> null;
        };
        return path == null ? null : "dataspec://evidence/" + path + "/" + sourceId;
    }

    private SourceLookup findSource(EvidenceSourceType sourceType, Long sourceId) {
        return switch (sourceType) {
            case SQL_CHECK -> sqlCheckRecordRepository.findById(sourceId)
                    .map(record -> SourceLookup.found(record.getProjectId()))
                    .orElseGet(SourceLookup::missing);
            case AI_JOB -> aiJobRecordRepository.findById(sourceId)
                    .map(record -> SourceLookup.found(record.getProjectId()))
                    .orElseGet(SourceLookup::missing);
            case AI_BATCH_RUN -> aiBatchRunRepository.findById(sourceId)
                    .map(run -> SourceLookup.found(run.getProjectId()))
                    .orElseGet(SourceLookup::missing);
            case AI_TASK_RUN -> aiTaskRunRepository.findById(sourceId)
                    .map(run -> SourceLookup.found(run.getProjectId()))
                    .orElseGet(SourceLookup::missing);
            case COVERAGE_REPORT -> SourceLookup.missing();
        };
    }

    private EvidenceSourceType sourceType(String path) {
        return switch (path) {
            case "sql-check" -> EvidenceSourceType.SQL_CHECK;
            case "ai-job" -> EvidenceSourceType.AI_JOB;
            case "ai-batch-run" -> EvidenceSourceType.AI_BATCH_RUN;
            case "ai-task-run" -> EvidenceSourceType.AI_TASK_RUN;
            default -> null;
        };
    }

    private Optional<Long> parsePositiveLong(String value) {
        try {
            return Optional.of(Long.valueOf(value));
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
    }

    private EvidenceClaimResolution result(
            String inputRef,
            String canonicalRef,
            EvidenceClaimResolutionStatus status,
            EvidenceSourceType sourceType,
            Long sourceId,
            Long sourceProjectId
    ) {
        return new EvidenceClaimResolution(
                inputRef,
                canonicalRef,
                status,
                sourceType,
                sourceId,
                sourceProjectId);
    }

    private record SourceLookup(boolean exists, Long projectId) {
        static SourceLookup found(Long projectId) {
            return new SourceLookup(true, projectId);
        }

        static SourceLookup missing() {
            return new SourceLookup(false, null);
        }
    }
}
