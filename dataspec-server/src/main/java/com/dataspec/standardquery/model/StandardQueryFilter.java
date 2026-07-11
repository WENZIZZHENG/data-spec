package com.dataspec.standardquery.model;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Standard Query DSL 的单个过滤条件。
 *
 * @param field 允许筛选的标准对象字段名；v1 仅支持字段标准的 allowlist 字段，不执行任意表达式。
 * @param op allowlist 操作符，如 eq、contains 或 gte；为空时按字段默认操作符处理。
 * @param value 过滤值；输出摘要会脱敏，且不会拼接为 SQL。
 */
@Schema(description = "Standard Query DSL 单个过滤条件；仅支持 allowlist 字段和操作符，不执行任意表达式。")
public record StandardQueryFilter(
        @Schema(description = "过滤字段名；v1 支持 category、tag、status、sensitive、sourceBatchId、stableRef、canonicalRef、hasExample、updatedSince。")
        String field,
        @Schema(description = "过滤操作符；支持 eq、contains、gte，未传时由字段默认语义决定。")
        String op,
        @Schema(description = "过滤值；视为敏感输入，错误和摘要只输出脱敏值。")
        Object value
) {
}
