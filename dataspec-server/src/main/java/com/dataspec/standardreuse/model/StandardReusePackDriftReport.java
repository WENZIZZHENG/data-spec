package com.dataspec.standardreuse.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 标准复用包漂移报告。
 */
@Schema(description = "目标项目相对标准复用包的轻量漂移报告。")
public record StandardReusePackDriftReport(
        @Schema(description = "漂移报告 schema 版本。") Integer schemaVersion,
        @Schema(description = "标准复用包 ID。") Long packId,
        @Schema(description = "目标项目 ID。") Long targetProjectId,
        @Schema(description = "复用包 key。") String packKey,
        @Schema(description = "复用包版本。") String basePackVersion,
        @Schema(description = "漂移计数。") StandardReusePackDriftCounts counts,
        @Schema(description = "漂移明细。") List<StandardReusePackPlanItem> items
) {
}
