package com.dataspec.field.model;

import com.dataspec.explaintrace.model.ExplainTrace;
import com.dataspec.field.entity.Field;

import java.util.List;

/**
 * 单个字段标准检索命中项。
 */
public record FieldSearchItem(
        Field field,
        int score,
        List<String> matchReasons,
        String recommendedUse,
        List<String> nextActions,
        List<ExplainTrace> evidence
) {
    public FieldSearchItem(Field field, int score, List<String> matchReasons, String recommendedUse, List<String> nextActions) {
        this(field, score, matchReasons, recommendedUse, nextActions, List.of());
    }
}
