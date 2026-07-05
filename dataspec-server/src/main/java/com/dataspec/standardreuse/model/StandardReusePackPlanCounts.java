package com.dataspec.standardreuse.model;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 标准复用包应用计划计数。
 */
@Schema(description = "标准复用包应用预览中的动作计数。")
public record StandardReusePackPlanCounts(
        @Schema(description = "将创建的顶层资产数量。") Integer created,
        @Schema(description = "内容一致并跳过的顶层资产数量。") Integer skipped,
        @Schema(description = "目标项目存在本地覆盖的顶层资产数量。") Integer overridden,
        @Schema(description = "目标项目与包内容不同的顶层资产数量。") Integer drifted,
        @Schema(description = "因缺少自然键或包异常而阻塞的顶层资产数量。") Integer blocked,
        @Schema(description = "非阻断警告数量。") Integer warnings
) {
    public static StandardReusePackPlanCounts empty() {
        return new StandardReusePackPlanCounts(0, 0, 0, 0, 0, 0);
    }
}
