package com.dataspec.tablemodel.model;

import java.util.List;

/**
 * 项目业务对象与表模板的只读关系摘要。
 *
 * @param projectId 项目 ID
 * @param nodes 对象、模板和字段节点
 * @param edges 关系边
 * @param summary 摘要指标
 */
public record TableRelationSummary(
        Long projectId,
        List<TableRelationSummaryNode> nodes,
        List<TableRelationSummaryEdge> edges,
        TableRelationSummaryStats summary
) {
}
