package com.dataspec.testdata.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Map;

/**
 * 标准驱动测试数据包。
 *
 * @param kind 包类型，固定为 dataspec.standard-test-data-package。
 * @param schemaVersion 包结构版本。
 * @param projectId 所属项目 ID。
 * @param specHash 基于标准摘要、筛选参数和生成版本计算的确定性 hash。
 * @param generationParams 脱敏后的生成参数。
 * @param sourceSummary 来源标准摘要，说明是否使用 fallback。
 * @param testDataCases 字段级 valid/invalid/boundary 用例。
 * @param seedProfiles JSON、CSV、SQL seed/mock 草稿。
 * @param mockPayloads AI 或前端 mock 可复用的结构化 payload。
 * @param coverageReport 覆盖率与缺口报告。
 * @param diagnostics 生成诊断，包含 fallback、脱敏和缺口提示。
 * @param safety 只读和安全边界声明。
 * @param nextActions 后续人工审核和复用建议。
 */
@Schema(description = "标准驱动测试数据包；用于 AI、测试、mock、seed 草稿和边界用例复用。")
public record StandardTestDataPackage(
        @Schema(description = "包类型，固定为 dataspec.standard-test-data-package。")
        String kind,
        @Schema(description = "包结构版本。")
        int schemaVersion,
        @Schema(description = "所属项目 ID。")
        Long projectId,
        @Schema(description = "基于标准摘要、筛选参数和生成版本计算的确定性 hash。")
        String specHash,
        @Schema(description = "脱敏后的生成参数。")
        Map<String, Object> generationParams,
        @Schema(description = "来源标准摘要，说明是否使用 fallback。")
        TestDataSourceSummary sourceSummary,
        @Schema(description = "字段级 valid/invalid/boundary 用例。")
        List<TestDataCase> testDataCases,
        @Schema(description = "JSON、CSV、SQL seed/mock 草稿。")
        List<TestDataSeedProfile> seedProfiles,
        @Schema(description = "AI 或前端 mock 可复用的结构化 payload。")
        List<TestDataMockPayload> mockPayloads,
        @Schema(description = "覆盖率与缺口报告。")
        TestDataCoverageReport coverageReport,
        @Schema(description = "生成诊断，包含 fallback、脱敏和缺口提示。")
        List<TestDataDiagnostic> diagnostics,
        @Schema(description = "只读和安全边界声明。")
        TestDataSafety safety,
        @Schema(description = "后续人工审核和复用建议。")
        List<String> nextActions
) {
}
