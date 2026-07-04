package com.dataspec.aitaskrun.model;

import com.dataspec.aitaskrun.entity.AiTaskRun;

import java.time.LocalDateTime;

/**
 * AI 任务列表摘要，避免列表接口返回完整 JSON payload。
 */
public record AiTaskRunListItem(
        Long id,
        Long projectId,
        String taskType,
        String sourceType,
        Long sourceId,
        String status,
        String inputHash,
        Boolean retryable,
        String failedStep,
        String resumeCommand,
        String nextAction,
        String operatorName,
        LocalDateTime startedAt,
        LocalDateTime finishedAt,
        LocalDateTime expiresAt,
        LocalDateTime createdAt
) {
    public static AiTaskRunListItem from(AiTaskRun run) {
        return new AiTaskRunListItem(
                run.getId(),
                run.getProjectId(),
                run.getTaskType(),
                run.getSourceType(),
                run.getSourceId(),
                run.getStatus(),
                run.getInputHash(),
                run.getRetryable(),
                run.getFailedStep(),
                run.getResumeCommand(),
                run.getNextAction(),
                run.getOperatorName(),
                run.getStartedAt(),
                run.getFinishedAt(),
                run.getExpiresAt(),
                run.getCreatedAt()
        );
    }
}
