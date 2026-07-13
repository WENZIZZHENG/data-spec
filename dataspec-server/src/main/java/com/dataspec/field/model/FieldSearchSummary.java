package com.dataspec.field.model;

import com.dataspec.standardquery.model.StandardQueryAppliedFilter;
import com.dataspec.standardquery.model.StandardQueryIgnoredFilter;
import com.dataspec.standardquery.model.StandardQuerySummary;
import com.dataspec.querynormalization.model.QueryTokenEvidence;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Map;

/**
 * 字段标准检索摘要，供 AI 判断结果是否完整或需要收窄条件。
 */
public record FieldSearchSummary(
        int totalCandidates,
        int matchedCount,
        int returnedCount,
        boolean truncated,
        Map<String, Object> appliedFilters,
        List<String> hints,
        @Schema(description = "字段搜索映射到 Standard Query DSL 后的脱敏查询摘要；additive 字段，不改变旧字段语义。")
        StandardQuerySummary querySummary,
        @ArraySchema(schema = @Schema(description = "字段搜索对应的 DSL 已应用过滤条件；值已脱敏。"))
        List<StandardQueryAppliedFilter> dslAppliedFilters,
        @ArraySchema(schema = @Schema(description = "字段搜索对应的 DSL 忽略过滤条件；legacy 搜索通常为空。"))
        List<StandardQueryIgnoredFilter> dslIgnoredFilters,
        @ArraySchema(schema = @Schema(description = "字段搜索对应的 DSL 下一步查询建议；等价于 querySummary.nextQueryHints。"))
        List<String> nextQueryHints,
        @ArraySchema(
                arraySchema = @Schema(description = "字段搜索使用的确定性 query token 证据；文本已脱敏，数量有界。"),
                schema = @Schema(description = "单条字段搜索 query token 解析证据。"))
        List<QueryTokenEvidence> queryTokens
) {
    public FieldSearchSummary(
            int totalCandidates,
            int matchedCount,
            int returnedCount,
            boolean truncated,
            Map<String, Object> appliedFilters,
            List<String> hints
    ) {
        this(totalCandidates, matchedCount, returnedCount, truncated, appliedFilters, hints, null, List.of(), List.of(), List.of(), List.of());
    }

    public FieldSearchSummary(
            int totalCandidates,
            int matchedCount,
            int returnedCount,
            boolean truncated,
            Map<String, Object> appliedFilters,
            List<String> hints,
            StandardQuerySummary querySummary,
            List<StandardQueryAppliedFilter> dslAppliedFilters,
            List<StandardQueryIgnoredFilter> dslIgnoredFilters,
            List<String> nextQueryHints
    ) {
        this(
                totalCandidates,
                matchedCount,
                returnedCount,
                truncated,
                appliedFilters,
                hints,
                querySummary,
                dslAppliedFilters,
                dslIgnoredFilters,
                nextQueryHints,
                List.of());
    }
}
