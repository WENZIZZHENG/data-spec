package com.dataspec.fieldmerge.model;

/**
 * 标准字段合并预览中的单项变更。
 *
 * @param attribute     受影响属性名。
 * @param beforeValue   变更前值。
 * @param afterValue    变更后值或建议值。
 * @param migrationMode 迁移模式，`SAFE_MERGE` 表示可自动合并，`MANUAL_REVIEW` 表示仅提示。
 * @param description   面向用户和 AI 的变更说明。
 */
public record StandardFieldMergeChange(
        String attribute,
        Object beforeValue,
        Object afterValue,
        String migrationMode,
        String description
) {
}
