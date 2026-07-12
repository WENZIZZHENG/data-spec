package com.dataspec.testdata.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 标准测试数据包生成请求。
 *
 * @param projectId 所属项目 ID，生成过程只读取该项目标准元数据。
 * @param fieldNames 可选字段名筛选；为空时按项目标准字段和 maxFields 自动选择。
 * @param objectScenario 可选轻量对象场景，如 order、user、audit；仅影响 fallback 命名和 seed 草稿表名。
 * @param maxFields 最大选取字段数，服务端会按安全上限裁剪。
 * @param casesPerField 每个字段最多生成多少类用例，v1 最多 valid/invalid/boundary 三类。
 * @param seedRowCount mock/CSV/SQL seed 草稿行数，默认使用小样本。
 * @param dialect SQL seed 草稿方言提示；v1 只作为说明，不承诺可直接执行。
 */
@Schema(description = "标准测试数据包生成请求；只读取项目标准元数据，不采集真实业务数据行。")
public record StandardTestDataPackageReq(
        @Schema(description = "所属项目 ID，生成过程只读取该项目标准元数据。")
        Long projectId,
        @Schema(description = "可选字段名筛选；为空时按项目标准字段和 maxFields 自动选择。")
        List<String> fieldNames,
        @Schema(description = "可选轻量对象场景，如 order、user、audit；仅影响 fallback 命名和 seed 草稿表名。")
        String objectScenario,
        @Schema(description = "最大选取字段数，服务端会按安全上限裁剪。")
        Integer maxFields,
        @Schema(description = "每个字段最多生成多少类用例，v1 最多 valid/invalid/boundary 三类。")
        Integer casesPerField,
        @Schema(description = "mock/CSV/SQL seed 草稿行数，默认使用小样本。")
        Integer seedRowCount,
        @Schema(description = "SQL seed 草稿方言提示；v1 只作为说明，不承诺可直接执行。")
        String dialect
) {
}
