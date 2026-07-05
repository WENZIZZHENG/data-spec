package com.dataspec.standardreuse.model;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 标准复用包确认应用结果。
 */
@Schema(description = "标准复用包确认应用后的计划和应用记录。")
public record StandardReusePackApplyResult(
        @Schema(description = "实际应用使用的计划。") StandardReusePackPlan plan,
        @Schema(description = "落库后的应用摘要。") StandardReusePackApplicationInfo application
) {
}
