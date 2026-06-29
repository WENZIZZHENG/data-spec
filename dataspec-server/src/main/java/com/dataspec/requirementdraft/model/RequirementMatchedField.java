package com.dataspec.requirementdraft.model;

import com.dataspec.field.entity.Field;

import java.util.List;

/**
 * 需求草案中建议采用的已有标准字段。
 */
public record RequirementMatchedField(
        Field field,
        int score,
        List<String> matchReasons,
        boolean recommended
) {
}
