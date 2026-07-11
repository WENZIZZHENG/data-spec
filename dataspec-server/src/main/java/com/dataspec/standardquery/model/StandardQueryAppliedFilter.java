package com.dataspec.standardquery.model;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 已应用的 Standard Query 过滤条件摘要。
 *
 * @param field allowlist 字段名。
 * @param op 生效操作符。
 * @param redactedValue 脱敏后的过滤值。
 * @param description 面向 AI/用户的过滤语义说明。
 */
@Schema(description = "已应用的 Standard Query 过滤条件摘要；值已脱敏，可安全出现在 API/CLI/MCP 输出。")
public record StandardQueryAppliedFilter(
        @Schema(description = "生效的 allowlist 字段名。")
        String field,
        @Schema(description = "生效操作符。")
        String op,
        @Schema(description = "脱敏后的过滤值。")
        String redactedValue,
        @Schema(description = "过滤语义说明。")
        String description
) {
}
