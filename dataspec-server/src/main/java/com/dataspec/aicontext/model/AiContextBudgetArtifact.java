package com.dataspec.aicontext.model;

/**
 * AI Context 预算计划中的单个 artifact 取舍。
 *
 * @param artifact artifact 路径或逻辑名称，不包含完整上下文内容
 * @param estimatedTokens artifact 的保守估算 token 数
 * @param reason 被选择或丢弃的原因摘要，不包含字段完整内容或敏感文本
 * @param riskImpact 该 artifact 被丢弃时的风险影响摘要
 * @param appliedScope 生成估算时应用的导出参数摘要
 */
public record AiContextBudgetArtifact(
        String artifact,
        int estimatedTokens,
        String reason,
        String riskImpact,
        AiContextRecommendedExportParams appliedScope
) {
}
