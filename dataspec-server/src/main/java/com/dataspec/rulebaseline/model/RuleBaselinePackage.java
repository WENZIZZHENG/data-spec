package com.dataspec.rulebaseline.model;

import java.time.LocalDateTime;
import java.util.List;

public record RuleBaselinePackage(
        Integer schemaVersion,
        RuleBaselineInfo baseline,
        LocalDateTime exportedAt,
        List<RuleBaselineRule> rules
) {
}
