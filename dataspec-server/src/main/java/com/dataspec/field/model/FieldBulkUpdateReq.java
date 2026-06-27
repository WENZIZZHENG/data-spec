package com.dataspec.field.model;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Map;

/**
 * 字段批量维护请求。
 *
 * <p>{@code updates} 使用显式 patch 语义：只更新出现的 key，key 的值为空字符串或 null 时表示清空对应可空字段。</p>
 */
public record FieldBulkUpdateReq(
        @NotNull(message = "项目ID不能为空") Long projectId,
        @NotEmpty(message = "字段ID不能为空") List<Long> fieldIds,
        @NotEmpty(message = "批量维护内容不能为空") Map<String, Object> updates
) {
}
