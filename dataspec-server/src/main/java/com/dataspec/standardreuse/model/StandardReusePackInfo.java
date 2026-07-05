package com.dataspec.standardreuse.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * 标准复用包列表摘要。
 */
@Schema(description = "标准复用包列表和详情的基础摘要。")
public record StandardReusePackInfo(
        @Schema(description = "标准复用包 ID。") Long packId,
        @Schema(description = "源项目 ID。") Long projectId,
        @Schema(description = "源项目名称快照。") String sourceProjectName,
        @Schema(description = "项目内稳定包 key。") String packKey,
        @Schema(description = "用户可读包名称。") String packName,
        @Schema(description = "用户定义共享包版本。") String basePackVersion,
        @Schema(description = "包说明。") String description,
        @Schema(description = "复用包 payload 的 SHA-256 hash。") String packageHash,
        @Schema(description = "包内资产数量。") StandardReusePackAssetCounts assetCounts,
        @Schema(description = "创建时间。") LocalDateTime createdAt
) {
}
