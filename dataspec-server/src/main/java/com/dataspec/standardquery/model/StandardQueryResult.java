package com.dataspec.standardquery.model;

import com.dataspec.field.model.FieldSearchItem;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Standard Query DSL 只读查询结果。
 *
 * @param projectId 当前项目 ID。
 * @param normalizedQuery 已归一化且脱敏的查询表达。
 * @param querySummary 查询摘要。
 * @param appliedFilters 已应用过滤条件。
 * @param ignoredFilters 被忽略过滤条件。
 * @param resultCount 命中总数。
 * @param returnedCount 返回条数。
 * @param truncated 是否截断。
 * @param nextQueryHints 下一步建议。
 * @param fields v1 字段标准命中项；target=FIELD 时返回。
 */
@Schema(description = "Standard Query DSL 只读查询结果；v1 目标为 FIELD 时返回字段标准命中项。")
public record StandardQueryResult(
        @Schema(description = "当前项目 ID。")
        Long projectId,
        @Schema(description = "已归一化且脱敏的查询表达。")
        StandardQueryNormalized normalizedQuery,
        @Schema(description = "查询执行摘要。")
        StandardQuerySummary querySummary,
        @ArraySchema(schema = @Schema(description = "已应用过滤条件。"))
        List<StandardQueryAppliedFilter> appliedFilters,
        @ArraySchema(schema = @Schema(description = "被忽略过滤条件。"))
        List<StandardQueryIgnoredFilter> ignoredFilters,
        @Schema(description = "命中总数。")
        int resultCount,
        @Schema(description = "返回条数。")
        int returnedCount,
        @Schema(description = "是否截断。")
        boolean truncated,
        @ArraySchema(schema = @Schema(description = "下一步查询建议。"))
        List<String> nextQueryHints,
        @ArraySchema(schema = @Schema(description = "字段标准命中项；v1 target=FIELD 时返回。"))
        List<FieldSearchItem> fields
) {
}
