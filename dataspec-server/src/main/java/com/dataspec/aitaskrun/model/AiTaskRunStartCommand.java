package com.dataspec.aitaskrun.model;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 内部使用的任务启动命令。
 */
public record AiTaskRunStartCommand(
        Long projectId,
        String taskType,
        String sourceType,
        String inputHash,
        String idempotencyKey,
        List<AiTaskStepStatus> stepStatus,
        List<AiTaskPartialArtifact> partialArtifacts,
        Map<String, Object> metadata,
        LocalDateTime expiresAt
) {
}
