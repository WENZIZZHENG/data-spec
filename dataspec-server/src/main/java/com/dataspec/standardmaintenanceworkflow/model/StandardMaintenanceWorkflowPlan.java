package com.dataspec.standardmaintenanceworkflow.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 标准维护 workflow dry-run 计划响应。
 *
 * @param projectId 计划所属项目 ID。
 * @param workflowId 本次 dry-run 计划 ID；不表示服务端持久化 workflow。
 * @param inboxAction 来源 Inbox 或诊断信号对应的建议动作。
 * @param recipeBinding AI workflow recipe 和 task-card 绑定信息。
 * @param dryRunSteps 按 precheck、review、execute、verify、archive 排列的计划步骤。
 * @param executionState dry-run 当前状态和恢复位置。
 * @param undoHint 未执行或中止时的安全回退说明。
 * @param evidenceLinks 支撑计划的脱敏证据链接和摘要。
 * @param nextActions 当前用户或 AI 可执行的下一步。
 */
@Schema(description = "标准维护 workflow dry-run 计划响应；该响应只描述计划，不执行写入。")
public record StandardMaintenanceWorkflowPlan(
        @Schema(description = "计划所属项目 ID。")
        Long projectId,
        @Schema(description = "本次 dry-run 计划 ID；不表示服务端持久化 workflow。")
        String workflowId,
        @Schema(description = "来源 Inbox 或诊断信号对应的建议动作。")
        StandardMaintenanceWorkflowInboxAction inboxAction,
        @Schema(description = "AI workflow recipe 和 task-card 绑定信息。")
        StandardMaintenanceWorkflowRecipeBinding recipeBinding,
        @Schema(description = "按 precheck、review、execute、verify、archive 排列的计划步骤。")
        List<StandardMaintenanceWorkflowStep> dryRunSteps,
        @Schema(description = "dry-run 当前状态和恢复位置。")
        StandardMaintenanceWorkflowExecutionState executionState,
        @Schema(description = "未执行或中止时的安全回退说明。")
        String undoHint,
        @Schema(description = "支撑计划的脱敏证据链接和摘要。")
        List<StandardMaintenanceWorkflowEvidenceLink> evidenceLinks,
        @Schema(description = "当前用户或 AI 可执行的下一步。")
        List<StandardMaintenanceWorkflowNextAction> nextActions
) {
}
