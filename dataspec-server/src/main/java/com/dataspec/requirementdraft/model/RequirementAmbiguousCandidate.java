package com.dataspec.requirementdraft.model;

import com.dataspec.field.entity.Field;

import java.util.List;

/**
 * 歧义词可能对应的已有标准字段。
 */
public record RequirementAmbiguousCandidate(
        Field field,
        int score,
        List<String> matchReasons
) {
}
