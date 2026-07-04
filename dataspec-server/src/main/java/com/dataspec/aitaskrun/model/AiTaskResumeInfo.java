package com.dataspec.aitaskrun.model;

/**
 * 可嵌入其他响应的任务恢复摘要。
 */
public record AiTaskResumeInfo(
        Long taskRunId,
        String status,
        Boolean retryable,
        String failedStep,
        String resumeCommand,
        String nextAction
) {
}
