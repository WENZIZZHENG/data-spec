package com.dataspec.tablemodel.model;

/**
 * 关系摘要边。
 *
 * @param source 来源节点 ID
 * @param target 目标节点 ID
 * @param kind 关系类型
 * @param confidence 置信度
 * @param evidence 非敏感证据说明
 */
public record TableRelationSummaryEdge(
        String source,
        String target,
        String kind,
        String confidence,
        String evidence
) {
}
