package com.dataspec.standardquery.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Standard Query DSL 校验错误契约。
 *
 * @param code 稳定错误码。
 * @param message 脱敏后的错误信息。
 * @param supportedFields 当前支持字段。
 * @param supportedOperators 当前支持操作符。
 * @param bounds 有界输入约束摘要。
 */
@Schema(description = "Standard Query DSL 校验错误契约；message 和建议均不得包含 raw secret。")
public record StandardQueryValidationError(
        @Schema(description = "稳定错误码。")
        String code,
        @Schema(description = "脱敏后的错误信息。")
        String message,
        @Schema(description = "当前支持的过滤字段。")
        List<String> supportedFields,
        @Schema(description = "当前支持的操作符。")
        List<String> supportedOperators,
        @Schema(description = "输入边界摘要，如最大文本长度、最大过滤条件数和最大 limit。")
        String bounds
) {
}
