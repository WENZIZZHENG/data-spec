package com.dataspec.requirementdraft.model;

import com.dataspec.explaintrace.model.ExplainTrace;

import java.util.List;

/**
 * 需求草案推荐的表模板摘要。
 */
public record RequirementRecommendedTemplate(
        Long id,
        String name,
        String description,
        String tablePrefix,
        int score,
        List<String> matchReasons,
        List<ExplainTrace> evidence
) {
    public RequirementRecommendedTemplate(Long id, String name, String description, String tablePrefix,
                                          int score, List<String> matchReasons) {
        this(id, name, description, tablePrefix, score, matchReasons, List.of());
    }
}
