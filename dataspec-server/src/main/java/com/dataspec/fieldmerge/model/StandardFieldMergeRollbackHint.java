package com.dataspec.fieldmerge.model;

/**
 * 标准字段合并回退提示。
 *
 * @param type        回退提示类型。
 * @param action      建议执行动作。
 * @param description 回退说明。
 * @param targetPath  可定位的 API 路径或页面路径。
 */
public record StandardFieldMergeRollbackHint(
        String type,
        String action,
        String description,
        String targetPath
) {
}
