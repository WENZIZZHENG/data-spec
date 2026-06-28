package com.dataspec.aibatch.model;

/**
 * 按规则聚合的问题统计。
 */
public record AiBatchIssueRuleSummary(
        String ruleCode,
        String ruleName,
        int count
) {
}
