package com.dataspec.rulebaseline.model;

import java.util.List;

public record RuleBaselineTemplate(
        String key,
        String name,
        String version,
        String description,
        Integer ruleCount,
        List<RuleBaselineRule> rules
) {
}
