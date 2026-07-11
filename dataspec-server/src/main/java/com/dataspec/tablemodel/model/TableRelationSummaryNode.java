package com.dataspec.tablemodel.model;

/**
 * 关系摘要节点。
 *
 * @param id 节点稳定 ID
 * @param type 节点类型，如 BUSINESS_OBJECT、TEMPLATE、FIELD
 * @param label 展示名称
 * @param refId 对应业务对象、模板或字段 ID
 */
public record TableRelationSummaryNode(
        String id,
        String type,
        String label,
        Long refId
) {
}
