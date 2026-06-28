package com.dataspec.lint.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 单次 SQL lint 的 fixedSql 生成策略。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FixPolicy {

    private FixMode mode;
    private FixRiskLevel maxRiskLevel;
    private List<String> enabledRuleCodes;
    private List<String> disabledRuleCodes;
    private Boolean includeExplanations;

    public static FixPolicy defaults() {
        return FixPolicy.builder()
                .mode(FixMode.GENERATE)
                .maxRiskLevel(FixRiskLevel.MEDIUM)
                .includeExplanations(true)
                .build();
    }

    public static FixPolicy effective(FixPolicy input) {
        FixPolicy defaults = defaults();
        if (input == null) {
            return defaults;
        }
        return FixPolicy.builder()
                .mode(input.mode == null ? defaults.mode : input.mode)
                .maxRiskLevel(input.maxRiskLevel == null ? defaults.maxRiskLevel : input.maxRiskLevel)
                .enabledRuleCodes(normalizeRules(input.enabledRuleCodes))
                .disabledRuleCodes(normalizeRules(input.disabledRuleCodes))
                .includeExplanations(input.includeExplanations == null
                        ? defaults.includeExplanations
                        : input.includeExplanations)
                .build();
    }

    public boolean dryRun() {
        return mode == FixMode.DRY_RUN;
    }

    public boolean disabled() {
        return mode == FixMode.DISABLED;
    }

    public boolean explanationsEnabled() {
        return !Boolean.FALSE.equals(includeExplanations);
    }

    public boolean allowsRule(String ruleCode) {
        String normalized = normalizeRule(ruleCode);
        Set<String> enabled = ruleSet(enabledRuleCodes);
        Set<String> disabled = ruleSet(disabledRuleCodes);
        return (enabled.isEmpty() || enabled.contains(normalized)) && !disabled.contains(normalized);
    }

    public boolean allowsRisk(FixRiskLevel riskLevel) {
        FixRiskLevel risk = riskLevel == null ? FixRiskLevel.HIGH : riskLevel;
        return risk.allowedBy(maxRiskLevel);
    }

    private static List<String> normalizeRules(List<String> rules) {
        if (rules == null) {
            return null;
        }
        return rules.stream()
                .map(FixPolicy::normalizeRule)
                .filter(rule -> !rule.isBlank())
                .distinct()
                .toList();
    }

    private static Set<String> ruleSet(List<String> rules) {
        if (rules == null || rules.isEmpty()) {
            return Set.of();
        }
        return rules.stream().collect(Collectors.toSet());
    }

    private static String normalizeRule(String ruleCode) {
        return ruleCode == null ? "" : ruleCode.trim().toLowerCase(Locale.ROOT);
    }
}
