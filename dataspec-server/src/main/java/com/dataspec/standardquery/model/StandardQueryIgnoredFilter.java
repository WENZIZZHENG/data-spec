package com.dataspec.standardquery.model;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 被忽略的 Standard Query 过滤条件摘要。
 *
 * @param field 原始字段名。
 * @param op 原始操作符。
 * @param redactedValue 脱敏后的原始值。
 * @param reason 脱敏后的忽略原因。
 */
@Schema(description = "被忽略的 Standard Query 过滤条件摘要；用于非 strict 模式解释降级原因。")
public record StandardQueryIgnoredFilter(
        @Schema(description = "原始过滤字段名。")
        String field,
        @Schema(description = "原始过滤操作符。")
        String op,
        @Schema(description = "脱敏后的过滤值。")
        String redactedValue,
        @Schema(description = "脱敏后的忽略原因。")
        String reason
) {
}
