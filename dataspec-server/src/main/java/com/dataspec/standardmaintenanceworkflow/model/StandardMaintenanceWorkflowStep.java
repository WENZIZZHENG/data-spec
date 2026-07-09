package com.dataspec.standardmaintenanceworkflow.model;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 标准维护 workflow dry-run 步骤。
 *
 * @param stepId 步骤稳定 ID，用于恢复当前位置。
 * @param phase 步骤阶段：precheck、review、execute、verify 或 archive。
 * @param title 人类可读步骤标题。
 * @param description 脱敏步骤说明。
 * @param recommendedAction 推荐执行的页面、API 或命令模板；必须由用户或 AI 显式运行。
 * @param requiresConfirmation 是否需要人工确认后才能执行推荐动作。
 * @param expectedEvidence 完成该步骤后应记录的证据摘要。
 * @param status dry-run 步骤状态，第一版通常为 PENDING。
 */
@Schema(description = "标准维护 workflow dry-run 步骤。")
public record StandardMaintenanceWorkflowStep(
        @Schema(description = "步骤稳定 ID，用于恢复当前位置。")
        String stepId,
        @Schema(description = "步骤阶段：precheck、review、execute、verify 或 archive。")
        String phase,
        @Schema(description = "人类可读步骤标题。")
        String title,
        @Schema(description = "脱敏步骤说明。")
        String description,
        @Schema(description = "推荐执行的页面、API 或命令模板；必须由用户或 AI 显式运行。")
        String recommendedAction,
        @Schema(description = "是否需要人工确认后才能执行推荐动作。")
        boolean requiresConfirmation,
        @Schema(description = "完成该步骤后应记录的证据摘要。")
        String expectedEvidence,
        @Schema(description = "dry-run 步骤状态，第一版通常为 PENDING。")
        String status
) {
}
