package com.dataspec.contractimport.model;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 契约候选预览摘要，用于 CLI 文本输出和 AI 快速判断结果规模。
 *
 * @param sourceFieldCount 从契约中识别到的原始字段数量。
 * @param candidateCount 去重和截断后返回的候选数量。
 * @param duplicateCount 同一契约内被合并的重复字段数量。
 * @param existingMatchCount 与已有标准字段命中的候选数量。
 * @param diagnosticCount 解析或安全诊断数量。
 * @param truncated 是否因为 maxCandidates 上限发生截断。
 */
@Schema(description = "契约候选预览摘要，用于 CLI 文本输出和 AI 快速判断结果规模。")
public record ContractCandidateSummary(
        @Schema(description = "从契约中识别到的原始字段数量，去重前计数。")
        int sourceFieldCount,
        @Schema(description = "去重和 maxCandidates 截断后返回的候选数量。")
        int candidateCount,
        @Schema(description = "同一契约内被合并的重复字段数量。")
        int duplicateCount,
        @Schema(description = "与已有标准字段命中的候选数量。")
        int existingMatchCount,
        @Schema(description = "解析、复杂 schema 或截断诊断数量。")
        int diagnosticCount,
        @Schema(description = "是否因为 maxCandidates 上限发生截断。")
        boolean truncated
) {
}
