package com.dataspec.standardquery.model;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Standard Query 执行摘要。
 *
 * @param target 查询目标类型。
 * @param text 脱敏后的检索文本。
 * @param resultCount 过滤和检索后命中的总数。
 * @param returnedCount 本次返回条数。
 * @param truncated 是否因 limit 截断。
 * @param nextQueryHints 下一步收窄、修正或改写查询的建议。
 */
@Schema(description = "Standard Query 执行摘要；用于 AI/CLI/MCP 判断查询是否完整或需要改写。")
public record StandardQuerySummary(
        @Schema(description = "查询目标类型。")
        String target,
        @Schema(description = "脱敏后的检索文本。")
        String text,
        @Schema(description = "过滤和检索后命中的总数。")
        int resultCount,
        @Schema(description = "本次返回条数。")
        int returnedCount,
        @Schema(description = "是否因 limit 截断。")
        boolean truncated,
        @ArraySchema(schema = @Schema(description = "下一步查询建议；不得包含 raw secret。"))
        List<String> nextQueryHints
) {
}
