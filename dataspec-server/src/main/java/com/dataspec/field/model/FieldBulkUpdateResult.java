package com.dataspec.field.model;

/**
 * 字段批量维护提交结果。
 */
public record FieldBulkUpdateResult(
        Long projectId,
        int requestedCount,
        int updatedCount,
        int unchangedCount
) {
}
