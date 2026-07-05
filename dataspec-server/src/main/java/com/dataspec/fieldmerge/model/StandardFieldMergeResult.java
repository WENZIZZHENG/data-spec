package com.dataspec.fieldmerge.model;

import java.util.List;

/**
 * 标准字段合并确认结果。
 *
 * @param kind          响应类型标识，便于 AI 判断载荷语义。
 * @param schemaVersion 合并结果响应 schema 版本。
 * @param projectId     DataSpec 项目 ID。
 * @param applied       true 表示合并已写入字段库。
 * @param preview       本次应用采用的合并预览。
 * @param rollbackHints 回退提示。
 * @param nextActions   下一步建议。
 */
public record StandardFieldMergeResult(
        String kind,
        int schemaVersion,
        Long projectId,
        boolean applied,
        StandardFieldMergePreview preview,
        List<StandardFieldMergeRollbackHint> rollbackHints,
        List<String> nextActions
) {
}
