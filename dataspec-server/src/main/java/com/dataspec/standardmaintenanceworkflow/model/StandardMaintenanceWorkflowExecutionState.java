package com.dataspec.standardmaintenanceworkflow.model;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 标准维护 workflow dry-run 的执行状态摘要。
 *
 * @param status 当前状态：DRY_RUN、BLOCKED、WAITING_CONFIRMATION 或 READY_FOR_REVIEW。
 * @param currentStepId 当前建议处理的步骤 ID。
 * @param retryable 阻塞或失败后是否可以在补充上下文后重新生成计划。
 * @param blockedReason 阻塞、partial 或失败来源说明；为空表示当前计划可继续人工执行。
 */
@Schema(description = "标准维护 workflow dry-run 的执行状态摘要。")
public record StandardMaintenanceWorkflowExecutionState(
        @Schema(description = "当前状态：DRY_RUN、BLOCKED、WAITING_CONFIRMATION 或 READY_FOR_REVIEW。")
        String status,
        @Schema(description = "当前建议处理的步骤 ID。")
        String currentStepId,
        @Schema(description = "阻塞或失败后是否可以在补充上下文后重新生成计划。")
        boolean retryable,
        @Schema(description = "阻塞、partial 或失败来源说明；为空表示当前计划可继续人工执行。")
        String blockedReason
) {
}
