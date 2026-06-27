package com.dataspec.field.model;

import java.util.List;

/**
 * 字段批量维护预览结果，不产生写入。
 */
public record FieldBulkUpdatePreview(
        Long projectId,
        int requestedCount,
        int changedCount,
        int unchangedCount,
        List<FieldBulkUpdateItem> items
) {
}
