package com.dataspec.aibatch.model;

/**
 * fixedSql 聚合摘要。
 */
public record AiBatchFixedSqlSummary(
        int availableCount,
        int changedCount
) {
}
