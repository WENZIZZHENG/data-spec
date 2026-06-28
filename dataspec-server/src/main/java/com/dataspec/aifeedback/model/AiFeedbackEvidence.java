package com.dataspec.aifeedback.model;

/**
 * 反馈信号的可解释证据来源。
 */
public record AiFeedbackEvidence(
        String sourceKind,
        Long sourceId,
        String description
) {
}
