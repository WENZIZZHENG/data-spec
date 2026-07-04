package com.dataspec.aitaskrun.model;

/**
 * AI 任务步骤状态摘要。
 */
public record AiTaskStepStatus(
        String step,
        String status,
        String message,
        String artifactRef
) {
}
