package com.dataspec.aibatch.model;

/**
 * 批量任务摘要，列表页和交付包共用。
 */
public record AiBatchSummary(
        int totalItems,
        int successItems,
        int failedItems,
        int errorCount,
        int warningCount,
        int suggestionCount,
        int fixedSqlCount
) {
    public static AiBatchSummary empty() {
        return new AiBatchSummary(0, 0, 0, 0, 0, 0, 0);
    }
}
