package com.dataspec.aioutputcheck.model;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * AI 输出后置校验摘要。
 *
 * @param totalRefCount 提取到并进入标准解析的引用总数。
 * @param currentCount 解析为当前可用标准对象的引用数。
 * @param staleCount 解析为废弃、停用或替代对象的引用数。
 * @param unknownCount 未知引用数。
 * @param ambiguousCount 歧义引用数。
 * @param crossProjectCount 跨项目引用数。
 * @param issueCount 问题总数，包含标准引用问题和证据缺口。
 */
@Schema(description = "AI 输出后置校验摘要，用于 AI/CLI/MCP 快速判断阻断和风险规模。")
public record AiOutputPostCheckSummary(
        @Schema(description = "提取到并进入标准解析的引用总数。")
        int totalRefCount,
        @Schema(description = "当前可用标准对象引用数。")
        int currentCount,
        @Schema(description = "废弃、停用或替代对象引用数。")
        int staleCount,
        @Schema(description = "未知引用数。")
        int unknownCount,
        @Schema(description = "歧义引用数。")
        int ambiguousCount,
        @Schema(description = "跨项目引用数。")
        int crossProjectCount,
        @Schema(description = "问题总数，包含证据缺口。")
        int issueCount
) {
}
