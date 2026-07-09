package com.dataspec.standardmaintenanceworkflow.model;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 标准维护 workflow 对 Inbox 或诊断来源建议采取的动作摘要。
 *
 * @param actionType 稳定动作类型，例如 REVIEW_CANDIDATES、REPAIR_FIELD_QUALITY。
 * @param sourceType 触发动作的来源类型。
 * @param targetCount 本次计划覆盖的待处理项数量。
 * @param title 人类可读标题。
 * @param description 脱敏动作说明，不包含 raw evidence 或凭据。
 * @param confirmationRequired 执行写入或状态决策前是否必须人工确认。
 */
@Schema(description = "标准维护 workflow 对 Inbox 或诊断来源建议采取的动作摘要。")
public record StandardMaintenanceWorkflowInboxAction(
        @Schema(description = "稳定动作类型，例如 REVIEW_CANDIDATES、REPAIR_FIELD_QUALITY。")
        String actionType,
        @Schema(description = "触发动作的来源类型。")
        String sourceType,
        @Schema(description = "本次计划覆盖的待处理项数量。")
        int targetCount,
        @Schema(description = "人类可读标题。")
        String title,
        @Schema(description = "脱敏动作说明，不包含 raw evidence 或凭据。")
        String description,
        @Schema(description = "执行写入或状态决策前是否必须人工确认。")
        boolean confirmationRequired
) {
}
