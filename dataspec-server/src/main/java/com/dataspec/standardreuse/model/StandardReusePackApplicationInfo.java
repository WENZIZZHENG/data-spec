package com.dataspec.standardreuse.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * 标准复用包应用摘要。
 */
@Schema(description = "目标项目应用标准复用包后的历史摘要。")
public record StandardReusePackApplicationInfo(
        @Schema(description = "应用记录 ID。") Long applicationId,
        @Schema(description = "目标项目 ID。") Long projectId,
        @Schema(description = "标准复用包 ID。") Long packId,
        @Schema(description = "复用包 key 快照。") String packKey,
        @Schema(description = "复用包名称快照。") String packName,
        @Schema(description = "复用包版本快照。") String basePackVersion,
        @Schema(description = "复用包 hash 快照。") String packageHash,
        @Schema(description = "源项目 ID 快照。") Long sourceProjectId,
        @Schema(description = "源项目名称快照。") String sourceProjectName,
        @Schema(description = "本次应用创建的资产计数。") StandardReusePackAssetCounts createdCounts,
        @Schema(description = "本次应用跳过的资产计数。") StandardReusePackAssetCounts skippedCounts,
        @Schema(description = "应用后的漂移计数。") StandardReusePackDriftCounts driftCounts,
        @Schema(description = "应用时间。") LocalDateTime appliedAt
) {
}
