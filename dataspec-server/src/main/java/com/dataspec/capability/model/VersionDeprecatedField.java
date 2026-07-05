package com.dataspec.capability.model;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 版本兼容握手中的废弃字段说明。
 *
 * <p>字段用于提醒 AI 和脚本迁移到新字段；第一版允许为空列表，不承诺无限期保留所有历史字段。</p>
 */
@Schema(description = "版本兼容握手中的废弃字段说明，用于 AI/CLI/MCP 迁移提示。")
public record VersionDeprecatedField(
        @Schema(description = "发生废弃的契约或 capability ID。")
        String contractId,
        @Schema(description = "废弃字段名或路径。")
        String field,
        @Schema(description = "字段开始废弃的 DataSpec 版本或日期。")
        String deprecatedSince,
        @Schema(description = "推荐替代字段；没有直接替代时为空。")
        String replacement,
        @Schema(description = "计划移除版本；未知时为空，调用方不得据此假设字段已移除。")
        String removeAfter,
        @Schema(description = "面向人和 AI 的迁移说明，不包含敏感值。")
        String note
) {
}
