package com.dataspec.standardusageheatmap.model;

/**
 * 标准使用热区项目级摘要。
 *
 * @param totalFieldCount 参与本次聚合的字段总数。
 * @param hotFieldCount `usageScore` 达到热区阈值的字段数。
 * @param riskyFieldCount `cleanupPriority` 达到治理优先级阈值的字段数。
 * @param cleanupCandidateCount 低使用且建议清理或归档的字段数。
 * @param fieldsWithoutSource 缺少来源证据的字段数。
 * @param averageCleanupPriority 字段平均治理优先级，字段为空时为 0。
 */
public record StandardUsageHeatmapSummary(
        int totalFieldCount,
        int hotFieldCount,
        int riskyFieldCount,
        int cleanupCandidateCount,
        int fieldsWithoutSource,
        int averageCleanupPriority
) {
}
