package com.dataspec.aibatch.model;

import java.util.List;

/**
 * 批量交付包的问题摘要。
 */
public record AiBatchIssueSummary(
        int errorCount,
        int warningCount,
        int suggestionCount,
        List<AiBatchIssueRuleSummary> byRule
) {
}
