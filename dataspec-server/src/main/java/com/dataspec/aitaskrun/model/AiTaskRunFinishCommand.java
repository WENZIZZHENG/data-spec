package com.dataspec.aitaskrun.model;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 内部使用的任务完成/失败更新命令。
 */
public record AiTaskRunFinishCommand(
        Long sourceId,
        Boolean retryable,
        String failedStep,
        String resumeCommand,
        String nextAction,
        List<AiTaskStepStatus> stepStatus,
        List<AiTaskPartialArtifact> partialArtifacts,
        Map<String, Object> metadata,
        LocalDateTime expiresAt
) {
}
