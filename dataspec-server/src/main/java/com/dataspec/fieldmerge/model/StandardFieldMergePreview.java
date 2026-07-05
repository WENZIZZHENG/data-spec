package com.dataspec.fieldmerge.model;

import java.util.List;

/**
 * 标准字段合并预览响应。
 *
 * @param kind                     响应类型标识，便于 AI 判断载荷语义。
 * @param schemaVersion            合并预览响应 schema 版本。
 * @param projectId                DataSpec 项目 ID。
 * @param recommendedTargetFieldId 推荐保留字段 ID，第一版等于用户选择的目标字段。
 * @param target                   保留字段当前摘要。
 * @param source                   来源字段当前摘要。
 * @param targetAfter              应用后的保留字段摘要。
 * @param sourceAfter              应用后的来源字段摘要。
 * @param changes                  字段级 diff 和迁移建议。
 * @param risks                    风险和阻断项。
 * @param impactItems              影响对象摘要。
 * @param rollbackHints            回退提示。
 * @param nextActions              下一步建议。
 */
public record StandardFieldMergePreview(
        String kind,
        int schemaVersion,
        Long projectId,
        Long recommendedTargetFieldId,
        StandardFieldMergeFieldSummary target,
        StandardFieldMergeFieldSummary source,
        StandardFieldMergeFieldSummary targetAfter,
        StandardFieldMergeFieldSummary sourceAfter,
        List<StandardFieldMergeChange> changes,
        List<StandardFieldMergeRisk> risks,
        List<StandardFieldMergeImpact> impactItems,
        List<StandardFieldMergeRollbackHint> rollbackHints,
        List<String> nextActions
) {
}
