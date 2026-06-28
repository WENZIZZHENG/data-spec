package com.dataspec.aifeedback.model;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 项目级 AI 使用反馈报告。
 */
public record AiFeedbackReport(
        Long projectId,
        AiFeedbackSummary summary,
        List<AiFeedbackSignal> fieldSignals,
        List<AiFeedbackSignal> ruleSignals,
        List<AiFeedbackSignal> fixedSqlSignals,
        List<AiFeedbackSignal> unmanagedSignals,
        List<AiFeedbackAction> nextActions,
        AiFeedbackSampleSize sampleSize,
        LocalDateTime generatedAt
) {
}
