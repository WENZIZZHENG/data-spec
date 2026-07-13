package com.dataspec.field.model;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 字段标准检索结果。
 *
 * @param projectId 字段所属项目 ID
 * @param query 脱敏后的查询文本
 * @param summary 匹配数量、过滤条件和确定性查询证据摘要
 * @param items 当前返回窗口中的字段匹配项
 * @param nextActions 面向用户或 AI 的后续动作
 * @param page 服务端分页元数据；legacy limit-only 调用为空
 */
@Schema(description = "项目级字段标准检索结果；只读且不会修改字段标准。")
public record FieldSearchResult(
        @Schema(description = "字段所属项目 ID。")
        Long projectId,
        @Schema(description = "脱敏后的查询文本；仅结构化过滤时可为空。")
        String query,
        @Schema(description = "匹配数量、过滤条件和确定性查询证据摘要。")
        FieldSearchSummary summary,
        @ArraySchema(
                arraySchema = @Schema(description = "当前返回窗口中的字段匹配项。"),
                schema = @Schema(implementation = FieldSearchItem.class))
        List<FieldSearchItem> items,
        @ArraySchema(
                arraySchema = @Schema(description = "面向用户或 AI 的安全后续动作。"),
                schema = @Schema(implementation = String.class))
        List<String> nextActions,
        @Schema(description = "服务端分页元数据；legacy limit-only 调用为空以保持原有语义。")
        FieldSearchPage page
) {
    /**
     * 创建 legacy limit-only 检索结果；分页元数据保持为空。
     */
    public FieldSearchResult(
            Long projectId,
            String query,
            FieldSearchSummary summary,
            List<FieldSearchItem> items,
            List<String> nextActions
    ) {
        this(projectId, query, summary, items, nextActions, null);
    }
}
