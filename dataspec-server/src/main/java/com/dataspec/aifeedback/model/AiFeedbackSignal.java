package com.dataspec.aifeedback.model;

import java.util.List;

/**
 * AI 反馈报告中的单个标准改进信号。
 */
public record AiFeedbackSignal(
        String signalType,
        String title,
        int count,
        String severity,
        List<AiFeedbackEvidence> evidence,
        String suggestedAction,
        String targetRoute
) {
}
