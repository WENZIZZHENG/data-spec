package com.dataspec.tablemodel.model;

/**
 * 软删除策略，只用于指导新表设计和 SQL 过滤，不自动改写历史查询。
 *
 * @param fieldName 软删除标记字段名
 * @param activeValue 未删除值
 * @param deletedValue 已删除值
 * @param defaultFilter 默认过滤说明
 * @param notes 非敏感说明
 */
public record TableSoftDeletePolicy(
        String fieldName,
        String activeValue,
        String deletedValue,
        String defaultFilter,
        String notes
) {
}
