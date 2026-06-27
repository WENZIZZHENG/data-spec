package com.dataspec.rulebaseline.model;

import java.time.LocalDateTime;

public record RuleBaselineInfo(
        Long projectId,
        String key,
        String name,
        String version,
        String source,
        LocalDateTime appliedAt,
        Integer ruleCount
) {
}
