package com.dataspec.standardreuse.model;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 标准复用包应用或漂移报告的单项。
 */
@Schema(description = "标准复用包应用计划或漂移报告中的单项动作。")
public record StandardReusePackPlanItem(
        @Schema(description = "资产类型，如 domain、field、enum_dict、rule、template。") String assetType,
        @Schema(description = "项目内自然键，如字段名、枚举编码或模板名。") String key,
        @Schema(description = "动作：CREATE、SKIP、OVERRIDDEN、DRIFTED、BLOCKED、MATCHED、MISSING。") String action,
        @Schema(description = "动作原因或人工处理提示。") String reason
) {
}
