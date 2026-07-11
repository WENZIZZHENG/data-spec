package com.dataspec.standardquery.model;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Standard Query DSL 只读查询请求。
 *
 * @param projectId 当前 DataSpec 项目 ID；查询必须限制在该项目内。
 * @param target 标准对象类型；v1 仅执行 FIELD，缺省时按 FIELD 处理。
 * @param text 自然语言或字段名检索文本；视为敏感输入，摘要和诊断会脱敏。
 * @param filters allowlist 过滤条件；不支持的条件在非 strict 模式进入 ignoredFilters。
 * @param sort 预留排序字段；v1 复用字段搜索既有排序，不执行任意排序表达式。
 * @param limit 返回上限；v1 复用字段搜索上限，最大 50。
 * @param explain 是否请求解释信息；v1 始终返回可解释摘要，false 仅表示调用方可忽略。
 * @param strict true 时遇到不支持 target/filter/op/value 即失败，false 时尽量降级并记录 ignoredFilters。
 */
@Schema(description = "Standard Query DSL 只读查询请求；用于 API/CLI/MCP/AI Context 复用同一字段标准筛选语义。")
public record StandardQueryRequest(
        @Schema(description = "当前 DataSpec 项目 ID；查询严格限制在该项目内。", example = "1")
        Long projectId,
        @Schema(description = "标准对象类型；v1 仅执行 FIELD，缺省时按 FIELD 处理。", example = "FIELD")
        String target,
        @Schema(description = "自然语言或字段名检索文本；视为敏感输入，输出摘要和错误会脱敏。")
        String text,
        @ArraySchema(schema = @Schema(description = "allowlist 过滤条件；不支持项在非 strict 模式进入 ignoredFilters。"))
        List<StandardQueryFilter> filters,
        @ArraySchema(schema = @Schema(description = "预留排序字段；v1 不执行任意排序表达式，保持字段搜索既有排序。"))
        List<String> sort,
        @Schema(description = "返回条数上限；v1 允许 1 到 50。", example = "20")
        Integer limit,
        @Schema(description = "是否请求解释信息；v1 始终返回可解释摘要。")
        Boolean explain,
        @Schema(description = "严格模式；true 时不支持的 target/filter/op/value 会在执行前失败。")
        Boolean strict
) {
}
