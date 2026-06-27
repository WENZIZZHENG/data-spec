package com.dataspec.field.model;

import java.util.List;

/**
 * 字段标准检索结果。
 */
public record FieldSearchResult(
        Long projectId,
        String query,
        FieldSearchSummary summary,
        List<FieldSearchItem> items,
        List<String> nextActions
) {
}
