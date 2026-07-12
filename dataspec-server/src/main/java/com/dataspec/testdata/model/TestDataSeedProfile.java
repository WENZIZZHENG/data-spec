package com.dataspec.testdata.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * seed 或 mock 草稿片段。
 *
 * @param profileId 确定性 profile ID。
 * @param format 输出格式，如 JSON、CSV、SQL。
 * @param dialect 方言提示；v1 不保证 SQL 可直接执行。
 * @param content 草稿文本，已脱敏。
 * @param fieldNames 参与该草稿的字段名。
 * @param sourceCaseIds 来源 case ID。
 * @param executable 草稿是否可直接执行；v1 SQL seed 默认为 false。
 * @param requiresReview 是否需要人工审核后再使用。
 */
@Schema(description = "seed 或 mock 草稿片段；默认只作可审查样例，不自动写入数据库。")
public record TestDataSeedProfile(
        @Schema(description = "确定性 profile ID。")
        String profileId,
        @Schema(description = "输出格式，如 JSON、CSV、SQL。")
        String format,
        @Schema(description = "方言提示；v1 不保证 SQL 可直接执行。")
        String dialect,
        @Schema(description = "草稿文本，已脱敏。")
        String content,
        @Schema(description = "参与该草稿的字段名。")
        List<String> fieldNames,
        @Schema(description = "来源 case ID。")
        List<String> sourceCaseIds,
        @Schema(description = "草稿是否可直接执行；v1 SQL seed 默认为 false。")
        boolean executable,
        @Schema(description = "是否需要人工审核后再使用。")
        boolean requiresReview
) {
}
