package com.dataspec.field.model;

/**
 * 字段批量维护中的单个属性变化。
 */
public record FieldBulkUpdateChange(
        String attribute,
        Object beforeValue,
        Object afterValue
) {
}
