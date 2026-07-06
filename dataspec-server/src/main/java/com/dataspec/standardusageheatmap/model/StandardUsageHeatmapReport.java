package com.dataspec.standardusageheatmap.model;

import java.util.List;

/**
 * 标准使用热区报告。
 *
 * @param projectId 报告所属项目 ID。
 * @param summary 项目级热区与治理摘要。
 * @param items 字段级热区列表，默认按治理优先级和使用热度排序。
 */
public record StandardUsageHeatmapReport(
        Long projectId,
        StandardUsageHeatmapSummary summary,
        List<StandardUsageHeatmapItem> items
) {
}
