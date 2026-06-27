package com.dataspec.field.model;

/**
 * 批量归组结果。
 */
public record FieldGroupingBatchUpdateResult(
        Long projectId,
        int requestedCount,
        int updatedCount
) {
}
