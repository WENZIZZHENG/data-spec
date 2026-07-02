package com.dataspec.requirementdraft.model;

import com.dataspec.explaintrace.model.ExplainTrace;
import com.dataspec.field.entity.Field;

import java.util.List;

/**
 * 需求草案中建议采用的已有标准字段。
 */
public record RequirementMatchedField(
        Field field,
        int score,
        List<String> matchReasons,
        boolean recommended,
        List<ExplainTrace> evidence
) {
    public RequirementMatchedField(Field field, int score, List<String> matchReasons, boolean recommended) {
        this(field, score, matchReasons, recommended, List.of());
    }
}
