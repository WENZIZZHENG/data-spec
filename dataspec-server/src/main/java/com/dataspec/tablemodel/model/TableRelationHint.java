package com.dataspec.tablemodel.model;

import java.util.List;

/**
 * 业务对象或表之间的轻量关系提示。
 *
 * @param sourceObjectKey 来源业务对象键
 * @param targetObjectKey 目标业务对象键
 * @param relationType 关系类型，如 ONE_TO_MANY、MANY_TO_ONE、ONE_TO_ONE
 * @param sourceColumns 来源列名数组
 * @param targetColumns 目标列名数组
 * @param optional 关系是否可选
 * @param confidence 置信度，建议 HIGH/MEDIUM/LOW
 * @param notes 非敏感说明
 */
public record TableRelationHint(
        String sourceObjectKey,
        String targetObjectKey,
        String relationType,
        List<String> sourceColumns,
        List<String> targetColumns,
        Boolean optional,
        String confidence,
        String notes
) {
}
