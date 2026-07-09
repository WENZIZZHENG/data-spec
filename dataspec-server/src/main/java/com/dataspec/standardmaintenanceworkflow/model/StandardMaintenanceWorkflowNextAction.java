package com.dataspec.standardmaintenanceworkflow.model;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 标准维护 workflow 的下一步提示。
 *
 * @param code 稳定动作代码，供前端或 AI 判断下一步类型。
 * @param severity 提示级别：info、warning 或 error。
 * @param message 脱敏人类可读说明。
 * @param command 可选命令或 API 模板，不包含凭据。
 * @param retryable 是否可在处理后重试生成计划。
 */
@Schema(description = "标准维护 workflow 的下一步提示。")
public record StandardMaintenanceWorkflowNextAction(
        @Schema(description = "稳定动作代码，供前端或 AI 判断下一步类型。")
        String code,
        @Schema(description = "提示级别：info、warning 或 error。")
        String severity,
        @Schema(description = "脱敏人类可读说明。")
        String message,
        @Schema(description = "可选命令或 API 模板，不包含凭据。")
        String command,
        @Schema(description = "是否可在处理后重试生成计划。")
        boolean retryable
) {
}
