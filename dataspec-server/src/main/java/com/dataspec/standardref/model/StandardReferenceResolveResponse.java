package com.dataspec.standardref.model;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 标准引用解析响应。
 *
 * @param kind 稳定响应类型标识，供 CLI/MCP/AI Context 判断契约。
 * @param schemaVersion 响应 schema 版本；breaking 变更必须升级。
 * @param projectId 当前解析所在项目。
 * @param results 按请求 refs 顺序返回的解析结果。
 * @param warnings 本次批量解析的脱敏提示。
 */
@Schema(description = "标准引用解析响应；保持请求顺序，且不会写入任何项目状态。")
public record StandardReferenceResolveResponse(
        @Schema(description = "稳定响应类型标识。", example = "dataspec-standard-reference-resolution")
        String kind,
        @Schema(description = "响应 schema 版本。", example = "1")
        int schemaVersion,
        @Schema(description = "当前项目 ID。", example = "1")
        Long projectId,
        @ArraySchema(schema = @Schema(description = "逐条引用解析结果。"))
        List<StandardReferenceResolutionResult> results,
        @ArraySchema(schema = @Schema(description = "批量解析级别脱敏提示。"))
        List<String> warnings
) {
    public static final String KIND = "dataspec-standard-reference-resolution";
    public static final int SCHEMA_VERSION = 1;

    public StandardReferenceResolveResponse {
        results = results == null ? List.of() : List.copyOf(results);
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
    }
}
