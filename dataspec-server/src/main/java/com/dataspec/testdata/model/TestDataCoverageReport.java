package com.dataspec.testdata.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 测试数据包覆盖报告。
 *
 * @param selectedFieldCount 选中的字段数。
 * @param coveredFieldCount 生成至少一个 case 的字段数。
 * @param caseCount 生成用例总数。
 * @param coverageLevel 覆盖级别，如 FIELD_ONLY 或 OBJECT_LIGHTWEIGHT。
 * @param missingConstraints 缺失或无法确定的约束说明。
 * @param unsupportedFields 暂不能生成确定性 case 的字段名。
 */
@Schema(description = "测试数据包覆盖报告，说明字段、case 和缺口。")
public record TestDataCoverageReport(
        @Schema(description = "选中的字段数。")
        int selectedFieldCount,
        @Schema(description = "生成至少一个 case 的字段数。")
        int coveredFieldCount,
        @Schema(description = "生成用例总数。")
        int caseCount,
        @Schema(description = "覆盖级别，如 FIELD_ONLY 或 OBJECT_LIGHTWEIGHT。")
        String coverageLevel,
        @Schema(description = "缺失或无法确定的约束说明。")
        List<String> missingConstraints,
        @Schema(description = "暂不能生成确定性 case 的字段名。")
        List<String> unsupportedFields
) {
}
