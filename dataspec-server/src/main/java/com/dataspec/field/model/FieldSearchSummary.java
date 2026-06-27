package com.dataspec.field.model;

import java.util.List;
import java.util.Map;

/**
 * 字段标准检索摘要，供 AI 判断结果是否完整或需要收窄条件。
 */
public record FieldSearchSummary(
        int totalCandidates,
        int matchedCount,
        int returnedCount,
        boolean truncated,
        Map<String, Object> appliedFilters,
        List<String> hints
) {
}
