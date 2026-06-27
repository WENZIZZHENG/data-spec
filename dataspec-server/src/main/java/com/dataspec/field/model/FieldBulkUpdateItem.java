package com.dataspec.field.model;

import java.util.List;

/**
 * 字段批量维护预览中的字段级变化。
 */
public record FieldBulkUpdateItem(
        Long fieldId,
        String fieldName,
        boolean changed,
        List<FieldBulkUpdateChange> changes
) {
}
