package com.dataspec.field.model;

/**
 * 字段变更回退结果。
 */
public record FieldChangeUndoResult(
        Long projectId,
        Long fieldId,
        Long logId
) {
}
