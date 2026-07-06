package com.dataspec.aicontext.model;

/**
 * AI Context 预算计划的质量风险等级。
 *
 * <p>枚举顺序从低到高，便于服务层在多个诊断之间取更高风险。</p>
 */
public enum AiContextBudgetQualityRisk {
    /**
     * 预算足以覆盖完整上下文，适合直接导出完整包。
     */
    LOW,

    /**
     * 预算只能覆盖关键上下文，适合收窄任务或使用 scoped export。
     */
    MEDIUM,

    /**
     * 预算不足以覆盖关键字段或规则上下文，AI 执行前需要提高预算或人工确认。
     */
    HIGH
}
