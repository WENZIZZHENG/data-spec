package com.dataspec.aitaskrun.model;

import com.dataspec.aitaskrun.entity.AiTaskRun;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * AI 任务详情，包含解析后的步骤和 artifact 摘要。
 */
public record AiTaskRunDetail(
        Long id,
        Long projectId,
        String taskType,
        String sourceType,
        Long sourceId,
        String status,
        String inputHash,
        String idempotencyKey,
        List<AiTaskStepStatus> stepStatus,
        Boolean retryable,
        String failedStep,
        String resumeCommand,
        String nextAction,
        List<AiTaskPartialArtifact> partialArtifacts,
        Map<String, Object> metadata,
        String operatorName,
        LocalDateTime startedAt,
        LocalDateTime finishedAt,
        LocalDateTime expiresAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static AiTaskRunDetail from(AiTaskRun run,
                                       List<AiTaskStepStatus> stepStatus,
                                       List<AiTaskPartialArtifact> partialArtifacts,
                                       Map<String, Object> metadata) {
        return new AiTaskRunDetail(
                run.getId(),
                run.getProjectId(),
                run.getTaskType(),
                run.getSourceType(),
                run.getSourceId(),
                run.getStatus(),
                run.getInputHash(),
                run.getIdempotencyKey(),
                stepStatus,
                run.getRetryable(),
                run.getFailedStep(),
                run.getResumeCommand(),
                run.getNextAction(),
                partialArtifacts,
                metadata,
                run.getOperatorName(),
                run.getStartedAt(),
                run.getFinishedAt(),
                run.getExpiresAt(),
                run.getCreatedAt(),
                run.getUpdatedAt()
        );
    }
}
