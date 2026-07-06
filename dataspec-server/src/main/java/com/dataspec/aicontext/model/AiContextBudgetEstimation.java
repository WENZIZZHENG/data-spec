package com.dataspec.aicontext.model;

/**
 * AI Context 预算估算摘要。
 *
 * @param tokenBudget 请求预算，单位为保守估算 token
 * @param selectedEstimatedTokens 当前计划选择的 artifact 估算 token 总数
 * @param totalEstimatedTokens 完整候选 artifact 的估算 token 总数
 * @param estimationMethod 估算方法说明；第一版为确定性本地估算，不代表模型 tokenizer 精确值
 * @param confidence 估算可信度摘要，用于提示 AI 这是预算等级判断而非精确 token 计数
 */
public record AiContextBudgetEstimation(
        int tokenBudget,
        int selectedEstimatedTokens,
        int totalEstimatedTokens,
        String estimationMethod,
        String confidence
) {
}
