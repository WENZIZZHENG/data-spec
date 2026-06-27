package com.dataspec.rulebaseline.model;

import java.util.List;

public record RuleBaselineApplyResult(
        Long projectId,
        RuleBaselineInfo baseline,
        Integer created,
        Integer updated,
        Integer skipped,
        List<String> createdRuleCodes,
        List<String> updatedRuleCodes,
        List<String> skippedRuleCodes
) {
}
