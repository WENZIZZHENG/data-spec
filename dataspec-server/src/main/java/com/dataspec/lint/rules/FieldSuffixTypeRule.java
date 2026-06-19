package com.dataspec.lint.rules;

import com.dataspec.lint.model.*;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 字段后缀/前缀类型校验。
 * <p>
 * 个人版第一阶段用固定默认值 + paramsJson 覆盖，避免过早引入复杂规则 DSL。
 */
@Component
public class FieldSuffixTypeRule implements LintRule {

    private static final Map<String, List<String>> DEFAULT_SUFFIX_TYPES = orderedMap(
            Map.entry("_id", List.of("bigint", "integer", "bigserial")),
            Map.entry("_at", List.of("timestamp", "timestamp with time zone", "datetime")),
            Map.entry("_no", List.of("varchar", "char", "text")),
            Map.entry("_count", List.of("integer", "bigint"))
    );

    private static final Map<String, List<String>> DEFAULT_PREFIX_TYPES = orderedMap(
            Map.entry("is_", List.of("boolean"))
    );

    @Override
    public String getCode() {
        return "field_suffix_type";
    }

    @Override
    public String getName() {
        return "字段后缀/前缀类型规则";
    }

    @Override
    public List<LintIssue> check(RuleContext context) {
        Map<String, List<String>> suffixTypes = resolveTypeRules(context.getRuleParams(), "suffixTypes", DEFAULT_SUFFIX_TYPES);
        Map<String, List<String>> prefixTypes = resolveTypeRules(context.getRuleParams(), "prefixTypes", DEFAULT_PREFIX_TYPES);

        List<LintIssue> issues = new ArrayList<>();
        for (TableDef table : context.getTables()) {
            for (ColumnDef column : table.getColumns()) {
                String columnName = column.getName().toLowerCase(Locale.ROOT);
                String dataType = normalizeType(column.getDataType());
                checkRuleMap(issues, table, column, dataType, suffixTypes, MatchMode.SUFFIX, columnName);
                checkRuleMap(issues, table, column, dataType, prefixTypes, MatchMode.PREFIX, columnName);
            }
        }
        return issues;
    }

    private void checkRuleMap(
            List<LintIssue> issues,
            TableDef table,
            ColumnDef column,
            String dataType,
            Map<String, List<String>> ruleMap,
            MatchMode mode,
            String columnName
    ) {
        for (Map.Entry<String, List<String>> entry : ruleMap.entrySet()) {
            if (entry.getValue().isEmpty()) {
                continue;
            }
            String pattern = entry.getKey().toLowerCase(Locale.ROOT);
            boolean matched = mode == MatchMode.SUFFIX
                    ? columnName.endsWith(pattern)
                    : columnName.startsWith(pattern);
            if (!matched || matchesAnyType(dataType, entry.getValue())) {
                continue;
            }
            String recommendedType = entry.getValue().getFirst();
            issues.add(LintIssue.builder()
                    .severity(Severity.WARNING)
                    .ruleCode(getCode())
                    .ruleName(getName())
                    .tableName(table.getName())
                    .columnName(column.getName())
                    .message(String.format("字段 '%s.%s' 命中命名模式 '%s'，类型应为 %s，当前为 %s",
                            table.getName(), column.getName(), entry.getKey(), entry.getValue(), column.getDataType()))
                    .suggestion(String.format("将字段 '%s' 的类型改为 '%s'", column.getName(), recommendedType))
                    .replacement(recommendedType)
                    .before(column.getDataType())
                    .after(recommendedType)
                    .confidence(75)
                    .build());
        }
    }

    private Map<String, List<String>> resolveTypeRules(
            Map<String, Object> params,
            String key,
            Map<String, List<String>> defaults
    ) {
        if (params == null || !params.containsKey(key) || !(params.get(key) instanceof Map<?, ?> map)) {
            return defaults;
        }
        Map<String, List<String>> resolved = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (entry.getKey() == null) {
                continue;
            }
            Object value = entry.getValue();
            if (value instanceof List<?> list) {
                resolved.put(entry.getKey().toString(), list.stream().map(Object::toString).toList());
            } else if (value instanceof String text) {
                resolved.put(entry.getKey().toString(), List.of(text));
            }
        }
        return resolved.isEmpty() ? defaults : resolved;
    }

    private boolean matchesAnyType(String actualType, List<String> allowedTypes) {
        for (String allowedType : allowedTypes) {
            if (actualType.equals(normalizeType(allowedType))) {
                return true;
            }
        }
        return false;
    }

    private String normalizeType(String type) {
        if (type == null) {
            return "";
        }
        String normalized = type.toLowerCase(Locale.ROOT)
                .replaceAll("\\bunsigned\\b", "")
                .replaceAll("\\s*\\([^)]*\\)", "")
                .replaceAll("\\s+", " ")
                .trim();
        if ("tinyint".equals(normalized)) {
            return "boolean";
        }
        if ("int".equals(normalized)) {
            return "integer";
        }
        return normalized;
    }

    private enum MatchMode {
        SUFFIX,
        PREFIX
    }

    @SafeVarargs
    private static Map<String, List<String>> orderedMap(Map.Entry<String, List<String>>... entries) {
        Map<String, List<String>> map = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> entry : entries) {
            map.put(entry.getKey(), entry.getValue());
        }
        return map;
    }
}
