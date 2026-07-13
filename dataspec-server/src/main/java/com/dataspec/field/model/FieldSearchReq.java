package com.dataspec.field.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

/**
 * 字段标准检索请求。
 *
 * <p>query 面向自然语言/字段名搜索；其余字段是确定性过滤条件。第一版保持只读，不触发候选写入。</p>
 */
public record FieldSearchReq(
        @Schema(description = "字段所属项目 ID，搜索结果不得跨项目。")
        Long projectId,
        @Schema(description = "字段名、显示名、别名或业务语义关键词；输出前会脱敏。")
        String query,
        @Schema(description = "字段分类精确过滤条件；为空表示不限制分类。")
        String category,
        @Schema(description = "字段标签精确过滤条件；为空表示不限制标签。")
        String tag,
        @Schema(description = "字段生命周期状态过滤条件，如 enabled、deprecated。")
        String status,
        @Schema(description = "是否敏感字段的精确过滤条件；为空表示不限制。")
        Boolean sensitive,
        @Schema(description = "反向导入来源批次 ID；仅返回该批次关联字段。")
        Long sourceBatchId,
        @Schema(description = "兼容旧调用的首批返回上限；仅在未提供 current/size 时生效，最大 50。")
        Integer limit,
        @Schema(description = "服务端分页页码，从 1 开始；提供 current 或 size 即进入分页模式。")
        Integer current,
        @Schema(description = "服务端分页每页条数，范围 1-100；提供 current 或 size 即进入分页模式。")
        Integer size,
        @Schema(description = "字段数据域 ID 精确过滤条件；为空表示不限制数据域。")
        Long domainId,
        @Schema(description = "true 仅返回未归入数据域、分类或标签的字段；false 仅返回已归组字段。")
        Boolean ungrouped,
        @Schema(description = "true 表示不应用 legacy 的 enabled 默认状态过滤；仅供显式展示全部生命周期状态的客户端使用。")
        Boolean includeAllStatuses,
        @Schema(description = "Standard Query 转换后的附加确定性过滤条件；legacy 调用通常为空。")
        Map<String, Object> extraFilters
) {
    /**
     * 创建 legacy limit-only 检索请求，保持既有 API、CLI 和 MCP 调用语义。
     */
    public FieldSearchReq(
            Long projectId,
            String query,
            String category,
            String tag,
            String status,
            Boolean sensitive,
            Long sourceBatchId,
            Integer limit
    ) {
        this(projectId, query, category, tag, status, sensitive, sourceBatchId, limit,
                null, null, null, null, null, Map.of());
    }

    /**
     * 创建带 Standard Query 附加过滤条件的 legacy 检索请求。
     */
    public FieldSearchReq(
            Long projectId,
            String query,
            String category,
            String tag,
            String status,
            Boolean sensitive,
            Long sourceBatchId,
            Integer limit,
            Map<String, Object> extraFilters
    ) {
        this(projectId, query, category, tag, status, sensitive, sourceBatchId, limit,
                null, null, null, null, null, extraFilters);
    }

    /**
     * 创建不含分组过滤条件的分页检索请求。
     */
    public FieldSearchReq(
            Long projectId,
            String query,
            String category,
            String tag,
            String status,
            Boolean sensitive,
            Long sourceBatchId,
            Integer limit,
            Integer current,
            Integer size
    ) {
        this(projectId, query, category, tag, status, sensitive, sourceBatchId, limit,
                current, size, null, null, null, Map.of());
    }

    /**
     * 创建字段库使用的分页检索请求，并支持数据域和归组状态过滤。
     */
    public FieldSearchReq(
            Long projectId,
            String query,
            String category,
            String tag,
            String status,
            Boolean sensitive,
            Long sourceBatchId,
            Integer limit,
            Integer current,
            Integer size,
            Long domainId,
            Boolean ungrouped
    ) {
        this(projectId, query, category, tag, status, sensitive, sourceBatchId, limit,
                current, size, domainId, ungrouped, null, Map.of());
    }

    /**
     * 创建字段库使用的分页检索请求，并显式控制是否包含全部生命周期状态。
     */
    public FieldSearchReq(
            Long projectId,
            String query,
            String category,
            String tag,
            String status,
            Boolean sensitive,
            Long sourceBatchId,
            Integer limit,
            Integer current,
            Integer size,
            Long domainId,
            Boolean ungrouped,
            Boolean includeAllStatuses
    ) {
        this(projectId, query, category, tag, status, sensitive, sourceBatchId, limit,
                current, size, domainId, ungrouped, includeAllStatuses, Map.of());
    }
}
