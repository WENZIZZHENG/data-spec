package com.dataspec.tablemodel.model;

/**
 * 关系摘要统计。
 *
 * @param objectCount 业务对象数量
 * @param templateCount 关联模板数量
 * @param edgeCount 关系边数量
 */
public record TableRelationSummaryStats(
        int objectCount,
        int templateCount,
        int edgeCount
) {
}
