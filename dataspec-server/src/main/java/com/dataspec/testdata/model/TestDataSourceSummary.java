package com.dataspec.testdata.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 测试数据包来源标准摘要。
 *
 * @param standardFieldCount 项目可用标准字段总数。
 * @param selectedFieldCount 本次选中字段数。
 * @param enumValueCount 使用到的枚举值数量。
 * @param fallbackUsed 是否使用内置 fallback 字段。
 * @param selectedFields 本次选中的字段名。
 * @param sourceKinds 来源类型，如 project-field、enum-value、fallback。
 */
@Schema(description = "测试数据包来源标准摘要，说明所用字段、枚举和 fallback。")
public record TestDataSourceSummary(
        @Schema(description = "项目可用标准字段总数。")
        int standardFieldCount,
        @Schema(description = "本次选中字段数。")
        int selectedFieldCount,
        @Schema(description = "使用到的枚举值数量。")
        int enumValueCount,
        @Schema(description = "是否使用内置 fallback 字段。")
        boolean fallbackUsed,
        @Schema(description = "本次选中的字段名。")
        List<String> selectedFields,
        @Schema(description = "来源类型，如 project-field、enum-value、fallback。")
        List<String> sourceKinds
) {
}
