package com.dataspec.projectbackup.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 项目备份恢复 dry-run 或应用后的计划摘要。
 *
 * @param dryRun true 表示只读预览，false 表示已经应用。
 * @param dryRunToken 预览计划对应的 evidence token，确认应用时必须带回；应用结果和恢复记录不返回该 token。
 * @param overwrite 是否允许覆盖同 key 资产。
 * @param canApply 当前计划是否可应用。
 * @param compatibilityStatus 备份包兼容状态。
 * @param targetProjectId 目标项目 ID；恢复到新项目的预览阶段可能为空。
 * @param targetProjectName 目标项目名称。
 * @param counts 本次计划的资产数量摘要。
 * @param items 逐项恢复动作。
 * @param warnings 需要用户或 AI 复核的风险提示。
 */
public record ProjectRestorePlan(
        @Schema(description = "true 表示只读预览，false 表示已经应用。")
        Boolean dryRun,
        @Schema(description = "预览计划对应的 evidence token，确认应用时必须带回；应用结果和恢复记录中为空，避免持久化写入证据。")
        String dryRunToken,
        @Schema(description = "是否允许覆盖同 key 资产。")
        Boolean overwrite,
        @Schema(description = "当前计划是否可应用。")
        Boolean canApply,
        @Schema(description = "备份包兼容状态。")
        String compatibilityStatus,
        @Schema(description = "目标项目 ID；恢复到新项目的预览阶段可能为空。")
        Long targetProjectId,
        @Schema(description = "目标项目名称。")
        String targetProjectName,
        @Schema(description = "本次计划的资产数量摘要。")
        ProjectRestoreCounts counts,
        @Schema(description = "逐项恢复动作。")
        List<ProjectRestoreItem> items,
        @Schema(description = "需要用户或 AI 复核的风险提示。")
        List<String> warnings
) {
    public ProjectRestorePlan(Boolean dryRun,
                              Boolean overwrite,
                              Boolean canApply,
                              String compatibilityStatus,
                              Long targetProjectId,
                              String targetProjectName,
                              ProjectRestoreCounts counts,
                              List<ProjectRestoreItem> items,
                              List<String> warnings) {
        this(dryRun, null, overwrite, canApply, compatibilityStatus, targetProjectId, targetProjectName, counts, items, warnings);
    }
}
