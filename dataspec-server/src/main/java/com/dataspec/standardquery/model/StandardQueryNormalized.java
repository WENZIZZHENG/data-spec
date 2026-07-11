package com.dataspec.standardquery.model;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 归一化后的 Standard Query DSL，可被 API、CLI、MCP 和 AI Context 复用。
 *
 * @param target 已归一化目标类型；v1 为 FIELD。
 * @param text 归一化后的检索文本，输出前已脱敏。
 * @param filters 已应用的归一化过滤条件。
 * @param sort 已接受的排序字段；v1 为空列表。
 * @param limit 生效的返回上限。
 * @param explain 是否返回解释。
 * @param strict 是否严格校验。
 */
@Schema(description = "归一化后的 Standard Query DSL；不包含 raw secret，可作为执行摘要和复用输入。")
public record StandardQueryNormalized(
        @Schema(description = "归一化标准对象类型；v1 为 FIELD。")
        String target,
        @Schema(description = "归一化并脱敏后的检索文本。")
        String text,
        @ArraySchema(schema = @Schema(description = "已应用的 allowlist 过滤条件摘要。"))
        List<StandardQueryAppliedFilter> filters,
        @ArraySchema(schema = @Schema(description = "已接受的排序字段；v1 保持空列表。"))
        List<String> sort,
        @Schema(description = "生效返回上限。")
        int limit,
        @Schema(description = "是否返回解释信息。")
        boolean explain,
        @Schema(description = "是否严格校验。")
        boolean strict
) {
}
