package com.dataspec.field.model;

import java.util.Map;

/**
 * 字段标准检索请求。
 *
 * <p>query 面向自然语言/字段名搜索；其余字段是确定性过滤条件。第一版保持只读，不触发候选写入。</p>
 */
public record FieldSearchReq(
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
        this(projectId, query, category, tag, status, sensitive, sourceBatchId, limit, Map.of());
    }
}
