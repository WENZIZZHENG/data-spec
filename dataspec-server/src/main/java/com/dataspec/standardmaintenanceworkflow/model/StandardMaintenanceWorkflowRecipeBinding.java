package com.dataspec.standardmaintenanceworkflow.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

/**
 * 标准维护 workflow 与 AI recipe/task-card 的绑定信息。
 *
 * @param recipeId workflow recipe id，第一版固定为 standard-maintenance。
 * @param recipeVersion recipe 契约版本，用于后续兼容升级。
 * @param sourceParameters 脱敏来源参数，供 AI task-card 或 CLI 生成计划时复用。
 * @param taskCardCommand 可复制的本地 task-card 命令模板，不包含 token、password 或连接串。
 */
@Schema(description = "标准维护 workflow 与 AI recipe/task-card 的绑定信息。")
public record StandardMaintenanceWorkflowRecipeBinding(
        @Schema(description = "workflow recipe id，第一版固定为 standard-maintenance。")
        String recipeId,
        @Schema(description = "recipe 契约版本，用于后续兼容升级。")
        int recipeVersion,
        @Schema(description = "脱敏来源参数，供 AI task-card 或 CLI 生成计划时复用。")
        Map<String, Object> sourceParameters,
        @Schema(description = "可复制的本地 task-card 命令模板，不包含 token、password 或连接串。")
        String taskCardCommand
) {
}
