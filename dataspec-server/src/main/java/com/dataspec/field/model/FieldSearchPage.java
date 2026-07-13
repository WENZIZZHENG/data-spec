package com.dataspec.field.model;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 字段搜索分页元数据；仅在调用方显式提供 current 或 size 时返回。
 *
 * @param current 当前页码，从 1 开始
 * @param size 当前页大小
 * @param total 确定性过滤和评分后的总命中数
 * @param pages 总页数；无命中时为 0
 * @param hasPrevious 是否存在上一页
 * @param hasNext 是否存在下一页
 */
@Schema(description = "字段搜索服务端分页元数据；legacy limit-only 调用不返回。")
public record FieldSearchPage(
        @Schema(description = "当前页码，从 1 开始。")
        int current,
        @Schema(description = "当前页大小，范围 1-100。")
        int size,
        @Schema(description = "确定性过滤和评分后的总命中数。")
        long total,
        @Schema(description = "总页数；无命中时为 0。")
        long pages,
        @Schema(description = "true 表示当前页之前仍有可访问结果。")
        boolean hasPrevious,
        @Schema(description = "true 表示当前页之后仍有可访问结果。")
        boolean hasNext
) {
}
