package com.dataspec.standardreuse.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 标准复用包应用预览计划。
 */
@Schema(description = "标准复用包应用 dry-run 计划。")
public record StandardReusePackPlan(
        @Schema(description = "响应类型标识。") String kind,
        @Schema(description = "响应 schema 版本。") Integer schemaVersion,
        @Schema(description = "标准复用包 ID。") Long packId,
        @Schema(description = "目标项目 ID。") Long targetProjectId,
        @Schema(description = "复用包 key。") String packKey,
        @Schema(description = "复用包版本。") String basePackVersion,
        @Schema(description = "是否可确认应用。") Boolean canApply,
        @Schema(description = "应用计划动作计数。") StandardReusePackPlanCounts counts,
        @Schema(description = "应用计划明细。") List<StandardReusePackPlanItem> items,
        @Schema(description = "非阻断警告。") List<String> warnings,
        @Schema(description = "目标项目相对包内容的漂移报告。") StandardReusePackDriftReport driftReport
) {
}
