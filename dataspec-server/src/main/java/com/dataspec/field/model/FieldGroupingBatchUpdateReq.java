package com.dataspec.field.model;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Map;

/**
 * 批量归组请求。
 *
 * <p>{@code updates} 使用显式 patch 语义：只更新出现的 key，key 的值为空字符串或 null 时表示清空对应分组字段。</p>
 */
public record FieldGroupingBatchUpdateReq(
        @NotNull(message = "项目ID不能为空") Long projectId,
        @NotEmpty(message = "字段ID不能为空") List<Long> fieldIds,
        @NotEmpty(message = "归组更新内容不能为空") Map<String, Object> updates
) {
}
