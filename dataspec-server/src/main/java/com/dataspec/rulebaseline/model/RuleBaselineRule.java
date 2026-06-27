package com.dataspec.rulebaseline.model;

public record RuleBaselineRule(
        String ruleCode,
        String ruleName,
        String severity,
        Boolean enabled,
        String paramsJson
) {
}
