package com.dataspec.lint.engine;

import com.dataspec.lint.model.ColumnDef;
import com.dataspec.lint.model.FixChange;
import com.dataspec.lint.model.FixChangeStatus;
import com.dataspec.lint.model.FixChangeType;
import com.dataspec.lint.model.FixPlanSummary;
import com.dataspec.lint.model.FixPolicy;
import com.dataspec.lint.model.FixRiskLevel;
import com.dataspec.lint.model.FixedSqlPlan;
import com.dataspec.lint.model.LintIssue;
import com.dataspec.lint.model.LintResult;
import com.dataspec.lint.model.TableDef;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 修正 SQL 生成器。
 * <p>
 * 基于 lint 解析出的 TableDef 与各规则的确定性修复建议(replacement/after),
 * 在内存中重建一份规范的 PostgreSQL CREATE TABLE + COMMENT ON 语句。
 * <p>
 * 设计取舍:不直接对原 SQL 文本做替换(缺乏 source span,易误伤同名字段),
 * 而是用解析模型重建,确保生成结果可被 DataSpec lint 自检通过。
 */
@Component
public class FixedSqlGenerator {

    private static final Pattern SAFE_IDENTIFIER = Pattern.compile("^[a-z][a-z0-9_]*$");
    private static final Set<String> FIELD_RENAME_RULES = Set.of(
            "field_naming_snake_case",
            "forbidden_field_name",
            "recommended_field_name"
    );
    private static final Set<String> UNSUPPORTED_FIXABLE_RULES = Set.of(
            "field_suffix_type",
            "amount_field_type"
    );

    /**
     * 兼容旧调用方:只返回修正后的 SQL。无法确定性重建时返回 null。
     */
    public String generate(LintResult result) {
        return generatePlan(result, FixPolicy.defaults()).getFixedSql();
    }

    /**
     * 生成修正 SQL 与机器可读修复计划。
     */
    public FixedSqlPlan generatePlan(LintResult result, FixPolicy requestedPolicy) {
        FixPolicy policy = FixPolicy.effective(requestedPolicy);
        List<TableDef> tables = result == null || result.getTables() == null ? List.of() : result.getTables();
        List<LintIssue> issues = result == null || result.getIssues() == null ? List.of() : result.getIssues();
        if (tables.isEmpty()) {
            return plan(policy, null, List.of(), List.of("未解析到可重建的 CREATE TABLE，已跳过 fixedSql。"));
        }

        List<CandidateFix> candidates = buildCandidates(issues, policy);
        resolveFieldRenameConflicts(candidates);
        List<FixChange> changes = changes(candidates);

        String fixedSql = null;
        if (!policy.disabled()) {
            FixIndex fixIndex = indexFixes(candidates);
            fixedSql = rebuildAll(tables, fixIndex);
            if (fixedSql == null) {
                markUnsafeRebuild(candidates);
                changes = changes(candidates);
            }
        }

        List<String> nextActions = nextActions(policy, fixedSql, changes);
        return plan(policy, fixedSql, changes, nextActions);
    }

    private FixedSqlPlan plan(FixPolicy policy, String fixedSql, List<FixChange> changes, List<String> nextActions) {
        List<FixChange> safeChanges = changes == null ? List.of() : changes;
        List<FixChange> explanations = policy.explanationsEnabled()
                ? safeChanges.stream()
                        .filter(change -> change.getStatus() == FixChangeStatus.SKIPPED)
                        .toList()
                : List.of();
        return FixedSqlPlan.builder()
                .fixPolicy(policy)
                .fixDryRun(policy.dryRun())
                .fixedSql(fixedSql)
                .fixChanges(safeChanges)
                .fixExplanations(explanations)
                .fixSummary(summary(safeChanges))
                .fixNextActions(nextActions)
                .build();
    }

    private FixPlanSummary summary(List<FixChange> changes) {
        int applied = 0;
        int planned = 0;
        int skipped = 0;
        for (FixChange change : changes) {
            if (change.getStatus() == FixChangeStatus.APPLIED) {
                applied += 1;
            } else if (change.getStatus() == FixChangeStatus.PLANNED) {
                planned += 1;
            } else if (change.getStatus() == FixChangeStatus.SKIPPED) {
                skipped += 1;
            }
        }
        return FixPlanSummary.builder()
                .availableCount(changes.size())
                .appliedCount(applied)
                .plannedCount(planned)
                .skippedCount(skipped)
                .build();
    }

    private List<String> nextActions(FixPolicy policy, String fixedSql, List<FixChange> changes) {
        List<String> actions = new ArrayList<>();
        if (policy.disabled()) {
            actions.add("fixedSql 已按策略关闭，请根据 issue 建议手动修复。");
        } else if (policy.dryRun()) {
            actions.add("当前为 dry-run 预览，应用 fixedSql 前请人工确认 diff 与风险。");
        } else if (fixedSql != null) {
            actions.add("应用 fixedSql 前请人工确认 diff、方言诊断和风险等级。");
        }
        if (changes.stream().anyMatch(change -> change.getStatus() == FixChangeStatus.SKIPPED)) {
            actions.add(policy.explanationsEnabled()
                    ? "存在未应用修复项，请查看 fixExplanations 后决定是否调整策略或人工修复。"
                    : "存在未应用修复项，可查看 fixChanges 状态或开启解释后再决定是否调整策略。");
        }
        return actions;
    }

    private List<CandidateFix> buildCandidates(List<LintIssue> issues, FixPolicy policy) {
        List<CandidateFix> candidates = new ArrayList<>();
        for (LintIssue issue : issues) {
            CandidateFix candidate = candidate(issue);
            if (candidate == null) {
                continue;
            }
            applyPolicy(candidate, policy);
            candidates.add(candidate);
        }
        return candidates;
    }

    private CandidateFix candidate(LintIssue issue) {
        if (issue == null) {
            return null;
        }
        String ruleCode = issue.getRuleCode();
        String tableLower = lower(issue.getTableName());
        if ("table_naming_snake_case".equals(ruleCode)
                && issue.getReplacement() != null
                && issue.getTableName() != null) {
            FixChange change = baseChange(issue, FixRiskLevel.LOW, FixChangeType.TABLE_RENAME,
                    issue.getTableName(), issue.getReplacement(), "按表名 snake_case 规则重命名表。");
            return new CandidateFix(issue, change, "table|" + tableLower, true);
        }

        if (FIELD_RENAME_RULES.contains(ruleCode)
                && issue.getReplacement() != null
                && issue.getTableName() != null
                && issue.getColumnName() != null) {
            FixChange change = baseChange(issue, FixRiskLevel.LOW, FixChangeType.COLUMN_RENAME,
                    issue.getColumnName(), issue.getReplacement(), "按字段命名规则重命名字段。");
            return new CandidateFix(issue, change,
                    "column|" + tableLower + "|" + lower(issue.getColumnName()), true);
        }

        if ("required_columns".equals(ruleCode)
                && issue.getReplacement() != null
                && issue.getAfter() != null
                && issue.getTableName() != null) {
            FixChange change = baseChange(issue, FixRiskLevel.MEDIUM, FixChangeType.REQUIRED_COLUMN_ADD,
                    null, issue.getAfter(), "按必备列规则补充缺失字段。");
            return new CandidateFix(issue, change,
                    "required|" + tableLower + "|" + lower(issue.getReplacement()), true);
        }

        if (UNSUPPORTED_FIXABLE_RULES.contains(ruleCode) && (issue.getReplacement() != null || issue.getAfter() != null)) {
            FixChange change = baseChange(issue, FixRiskLevel.HIGH, FixChangeType.UNSUPPORTED_RULE,
                    issue.getBefore(), firstNonBlank(issue.getAfter(), issue.getReplacement()),
                    "该规则涉及语义或类型变更，本轮只解释，不自动改写。");
            return new CandidateFix(issue, change,
                    "unsupported|" + lower(ruleCode) + "|" + tableLower + "|" + lower(issue.getColumnName()), false);
        }

        return null;
    }

    private FixChange baseChange(LintIssue issue, FixRiskLevel risk, FixChangeType type,
                                 String before, String after, String explain) {
        return FixChange.builder()
                .ruleCode(issue.getRuleCode())
                .ruleName(issue.getRuleName())
                .riskLevel(risk)
                .changeType(type)
                .tableName(issue.getTableName())
                .columnName(issue.getColumnName())
                .before(before)
                .after(after)
                .explain(explain)
                .confidence(issue.getConfidence())
                .sourceStart(issue.getSourceStart())
                .sourceEnd(issue.getSourceEnd())
                .build();
    }

    private void applyPolicy(CandidateFix candidate, FixPolicy policy) {
        FixChange change = candidate.change();
        LintIssue issue = candidate.issue();
        if (Boolean.TRUE.equals(issue.getSuppressed())) {
            skip(candidate, "SUPPRESSED", "该问题已被规则例外豁免，不参与 fixedSql。");
        } else if (policy.disabled()) {
            skip(candidate, "POLICY_DISABLED", "当前 fixPolicy.mode=DISABLED，仅返回解释。");
        } else if (!candidate.rebuildSupported()) {
            skip(candidate, "UNSUPPORTED_FIXER", change.getExplain());
        } else if (!policy.allowsRule(change.getRuleCode())) {
            skip(candidate, "RULE_FILTERED", "当前 fixPolicy 未允许该规则参与 fixedSql。");
        } else if (!policy.allowsRisk(change.getRiskLevel())) {
            skip(candidate, "RISK_EXCEEDS_POLICY", "该修复风险高于当前 fixPolicy.maxRiskLevel。");
        } else {
            change.setStatus(policy.dryRun() ? FixChangeStatus.PLANNED : FixChangeStatus.APPLIED);
            change.setReasonCode(null);
            annotateIssue(issue, change);
        }
    }

    private void resolveFieldRenameConflicts(List<CandidateFix> candidates) {
        Map<String, CandidateFix> selectedByKey = new HashMap<>();
        for (CandidateFix candidate : candidates) {
            if (!candidate.rebuildUsable() || !candidate.key().startsWith("column|")) {
                continue;
            }
            CandidateFix current = selectedByKey.get(candidate.key());
            if (current == null || confidence(candidate) > confidence(current)) {
                selectedByKey.put(candidate.key(), candidate);
            }
        }
        for (CandidateFix candidate : candidates) {
            if (!candidate.rebuildUsable() || !candidate.key().startsWith("column|")) {
                continue;
            }
            CandidateFix selected = selectedByKey.get(candidate.key());
            if (selected != candidate) {
                skip(candidate, "LOWER_CONFIDENCE", "同一字段存在多个修复候选，已选择置信度更高的建议。");
            }
        }
    }

    private int confidence(CandidateFix candidate) {
        Integer value = candidate.change().getConfidence();
        return value == null ? 0 : value;
    }

    private void markUnsafeRebuild(List<CandidateFix> candidates) {
        boolean changed = false;
        for (CandidateFix candidate : candidates) {
            if (candidate.rebuildUsable()) {
                skip(candidate, "UNSAFE_REBUILD", "fixedSql 重建阶段发现标识符、列冲突或片段不安全，已整体跳过。");
                changed = true;
            }
        }
        if (!changed) {
            // 没有候选时的格式化重建失败没有对应 issue，只能通过 nextActions 表达。
        }
    }

    private void skip(CandidateFix candidate, String reasonCode, String explain) {
        FixChange change = candidate.change();
        change.setStatus(FixChangeStatus.SKIPPED);
        change.setReasonCode(reasonCode);
        change.setExplain(explain);
        annotateIssue(candidate.issue(), change);
    }

    private void annotateIssue(LintIssue issue, FixChange change) {
        issue.setFixRiskLevel(change.getRiskLevel());
        issue.setFixChangeType(change.getChangeType());
        issue.setFixStatus(change.getStatus());
        issue.setFixExplain(change.getExplain());
        issue.setFixReasonCode(change.getReasonCode());
    }

    private List<FixChange> changes(List<CandidateFix> candidates) {
        return candidates.stream()
                .map(CandidateFix::change)
                .sorted(Comparator
                        .comparing((FixChange change) -> change.getStatus() == FixChangeStatus.SKIPPED ? 1 : 0)
                        .thenComparing(change -> nullToEmpty(change.getTableName()))
                        .thenComparing(change -> nullToEmpty(change.getColumnName()))
                        .thenComparing(change -> nullToEmpty(change.getRuleCode())))
                .toList();
    }

    private FixIndex indexFixes(List<CandidateFix> candidates) {
        Map<String, String> tableRenames = new HashMap<>();
        Map<String, FieldFix> fieldFixes = new HashMap<>();
        Map<String, Map<String, String>> missingRequired = new LinkedHashMap<>();

        for (CandidateFix candidate : candidates) {
            if (!candidate.rebuildUsable()) {
                continue;
            }
            FixChange change = candidate.change();
            String tableLower = lower(change.getTableName());
            if (change.getChangeType() == FixChangeType.TABLE_RENAME) {
                tableRenames.put(tableLower, change.getAfter());
            } else if (change.getChangeType() == FixChangeType.COLUMN_RENAME) {
                String key = tableLower + "|" + lower(change.getColumnName());
                fieldFixes.put(key, new FieldFix(change.getAfter(), change.getConfidence()));
            } else if (change.getChangeType() == FixChangeType.REQUIRED_COLUMN_ADD) {
                missingRequired
                        .computeIfAbsent(tableLower, k -> new LinkedHashMap<>())
                        .put(lower(candidate.issue().getReplacement()), change.getAfter());
            }
        }
        return new FixIndex(tableRenames, fieldFixes, missingRequired);
    }

    private String rebuildAll(List<TableDef> tables, FixIndex fixIndex) {
        StringBuilder sb = new StringBuilder();
        for (TableDef table : tables) {
            String block = rebuildTable(table, fixIndex);
            if (block == null) {
                return null;
            }
            if (!sb.isEmpty()) {
                sb.append("\n");
            }
            sb.append(block);
        }
        return sb.isEmpty() ? null : sb.toString();
    }

    /**
     * 重建单表的 CREATE TABLE + COMMENT ON。
     * 返回 null 表示无法安全重建(如重命名后标识符非法、类型片段异常)。
     */
    private String rebuildTable(TableDef table, FixIndex fixIndex) {
        String originalName = table.getName();
        if (originalName == null) {
            return null;
        }
        String tableLower = originalName.toLowerCase(Locale.ROOT);
        String targetName = fixIndex.tableRenames().getOrDefault(tableLower, originalName);
        if (!SAFE_IDENTIFIER.matcher(targetName).matches()) {
            return null;
        }

        List<ColumnDef> originalColumns = table.getColumns() != null ? table.getColumns() : List.of();
        List<ColumnDef> finalColumns = new ArrayList<>();
        Map<String, String> finalColumnComments = new LinkedHashMap<>();
        Set<String> finalColumnNamesLower = new HashSet<>();

        for (ColumnDef col : originalColumns) {
            if (col.getName() == null) {
                return null;
            }
            String fixKey = tableLower + "|" + col.getName().toLowerCase(Locale.ROOT);
            FieldFix fix = fixIndex.fieldFixes().get(fixKey);
            String finalName = fix != null ? fix.replacement() : col.getName();
            if (!SAFE_IDENTIFIER.matcher(finalName).matches()) {
                return null;
            }
            String finalNameLower = finalName.toLowerCase(Locale.ROOT);
            if (finalColumnNamesLower.contains(finalNameLower)) {
                return null;
            }
            finalColumnNamesLower.add(finalNameLower);

            ColumnDef rebuilt = ColumnDef.builder()
                    .name(finalName)
                    .dataType(col.getDataType())
                    .nullable(col.isNullable())
                    .defaultValue(col.getDefaultValue())
                    .comment(col.getComment())
                    .build();
            finalColumns.add(rebuilt);
            finalColumnComments.put(finalNameLower, col.getComment());
        }

        Map<String, String> missing = fixIndex.missingRequired().get(tableLower);
        if (missing != null) {
            for (Map.Entry<String, String> entry : missing.entrySet()) {
                String requiredNameLower = entry.getKey();
                if (finalColumnNamesLower.contains(requiredNameLower)) {
                    continue;
                }
                ColumnDef requiredCol = parseColumnSnippet(entry.getValue());
                if (requiredCol == null) {
                    return null;
                }
                finalColumns.add(requiredCol);
                finalColumnComments.put(requiredNameLower, null);
                finalColumnNamesLower.add(requiredNameLower);
            }
        }

        return renderTable(targetName, table.getComment(), finalColumns, finalColumnComments);
    }

    /**
     * 解析 RequiredColumnsRule 的 after 片段(如 “id bigserial PRIMARY KEY”)为 ColumnDef。
     * 保留多词类型、NOT NULL 和 DEFAULT,无法解析返回 null。
     */
    private ColumnDef parseColumnSnippet(String snippet) {
        if (snippet == null || snippet.isBlank()) {
            return null;
        }
        String trimmed = snippet.trim();
        int spaceIdx = trimmed.indexOf(' ');
        if (spaceIdx <= 0) {
            return null;
        }
        String name = trimmed.substring(0, spaceIdx);
        String remainder = trimmed.substring(spaceIdx + 1).trim();
        int constraintStart = findFirstConstraintIndex(remainder);
        String cleanType = constraintStart >= 0 ? remainder.substring(0, constraintStart).trim() : remainder;
        if (!SAFE_IDENTIFIER.matcher(name).matches()
                || cleanType.isBlank()
                || !cleanType.matches("[a-zA-Z][a-zA-Z0-9_ ]*(\\([^)]+\\))?")) {
            return null;
        }
        String constraints = constraintStart >= 0 ? remainder.substring(constraintStart).trim() : "";
        String lowerConstraints = constraints.toLowerCase(Locale.ROOT);
        boolean nullable = !(lowerConstraints.contains("not null") || lowerConstraints.contains("primary key"));
        String defaultValue = extractDefaultValue(constraints);
        return ColumnDef.builder()
                .name(name)
                .dataType(cleanType)
                .nullable(nullable)
                .defaultValue(defaultValue)
                .comment(null)
                .build();
    }

    private int findFirstConstraintIndex(String value) {
        String lower = value.toLowerCase(Locale.ROOT);
        int result = -1;
        for (String token : List.of(" primary key", " not null", " default ", " unique", " references ")) {
            int index = lower.indexOf(token);
            if (index >= 0 && (result < 0 || index < result)) {
                result = index;
            }
        }
        return result;
    }

    private String extractDefaultValue(String constraints) {
        String lower = constraints.toLowerCase(Locale.ROOT);
        int index = lower.indexOf(" default ");
        if (index < 0) {
            return null;
        }
        String defaultValue = constraints.substring(index + " default ".length()).trim();
        return defaultValue.isBlank() ? null : defaultValue;
    }

    private String renderTable(String tableName, String tableComment,
                               List<ColumnDef> columns, Map<String, String> columnComments) {
        StringBuilder sb = new StringBuilder();
        sb.append("CREATE TABLE ").append(tableName).append(" (\n");
        for (int i = 0; i < columns.size(); i++) {
            ColumnDef col = columns.get(i);
            sb.append("    ").append(col.getName()).append(" ").append(col.getDataType());
            if (!col.isNullable()) {
                sb.append(" NOT NULL");
            }
            if (col.getDefaultValue() != null && !col.getDefaultValue().isBlank()) {
                sb.append(" DEFAULT ").append(col.getDefaultValue());
            }
            if (i < columns.size() - 1) {
                sb.append(",");
            }
            sb.append("\n");
        }
        sb.append(");\n");

        if (tableComment != null && !tableComment.isBlank()) {
            sb.append("COMMENT ON TABLE ").append(tableName)
                    .append(" IS '").append(escapeSqlLiteral(tableComment)).append("';\n");
        }
        for (ColumnDef col : columns) {
            String comment = columnComments.get(col.getName().toLowerCase(Locale.ROOT));
            if (comment != null && !comment.isBlank()) {
                sb.append("COMMENT ON COLUMN ").append(tableName).append(".").append(col.getName())
                        .append(" IS '").append(escapeSqlLiteral(comment)).append("';\n");
            }
        }
        return sb.toString();
    }

    private String escapeSqlLiteral(String value) {
        return value.replace("'", "''");
    }

    private String lower(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private String firstNonBlank(String first, String second) {
        return first != null && !first.isBlank() ? first : second;
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private record CandidateFix(LintIssue issue, FixChange change, String key, boolean rebuildSupported) {
        boolean rebuildUsable() {
            return rebuildSupported
                    && (change.getStatus() == FixChangeStatus.APPLIED
                    || change.getStatus() == FixChangeStatus.PLANNED);
        }
    }

    /** 单个字段的修复建议(replacement + 置信度) */
    private record FieldFix(String replacement, Integer confidence) {
    }

    /** 预索引的修复集合 */
    private record FixIndex(
            Map<String, String> tableRenames,
            Map<String, FieldFix> fieldFixes,
            Map<String, Map<String, String>> missingRequired
    ) {
    }
}
