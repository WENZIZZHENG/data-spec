package com.dataspec.aifeedback.model;

/**
 * AI 使用反馈项目级摘要。
 */
public record AiFeedbackSummary(
        int aiJobCount,
        int sqlCheckCount,
        int ruleExemptionCount,
        int fieldSourceCount,
        int fieldSignalCount,
        int ruleSignalCount,
        int fixedSqlAvailableCount,
        boolean insufficientSuggestionHistory,
        String recommendationHistoryNote
) {
}
