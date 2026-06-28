package com.dataspec.aifeedback.model;

/**
 * 报告样本量，帮助 AI 和用户理解统计边界。
 */
public record AiFeedbackSampleSize(
        int aiJobRecords,
        int sqlCheckRecords,
        int ruleExemptions,
        int fieldSources,
        int fields
) {
}
