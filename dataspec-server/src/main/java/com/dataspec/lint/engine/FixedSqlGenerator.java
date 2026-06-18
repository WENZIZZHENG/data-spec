package com.dataspec.lint.engine;

import com.dataspec.lint.model.ColumnDef;
import com.dataspec.lint.model.LintIssue;
import com.dataspec.lint.model.LintResult;
import com.dataspec.lint.model.TableDef;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
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
 * 第一版只做确定性改写:表名/字段名 snake_case、禁用/推荐字段名替换、补必备列;
 * 金额字段类型等语义改写留给 AI。
 */
@Component
public class FixedSqlGenerator {

    private static final Pattern SAFE_IDENTIFIER = Pattern.compile("^[a-z][a-z0-9_]*$");

    /**
     * 生成修正后的 SQL。无法确定性重建时返回 null。
     */
    public String generate(LintResult result) {
        if (result == null || result.getTables() == null || result.getTables().isEmpty()) {
            return null;
        }

        FixIndex fixIndex = indexFixes(result.getIssues());

        StringBuilder sb = new StringBuilder();
        for (TableDef table : result.getTables()) {
            String block = rebuildTable(table, fixIndex);
            if (block == null) {
                // 任一表无法安全重建则整体放弃,保持“不生成危险 SQL”原则
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
     * 把扁平的 issues 索引成“按表/列可查询”的修复映射,便于重建阶段快速定位。
     */
    private FixIndex indexFixes(List<LintIssue> issues) {
        Map<String, String> tableRenames = new HashMap<>();
        // key: 表名(小写) + "|" + 原字段名(小写)
        Map<String, FieldFix> fieldFixes = new HashMap<>();
        // key: 表名(小写),value: 缺失的必备列名 → 完整列定义片段
        Map<String, Map<String, String>> missingRequired = new LinkedHashMap<>();

        if (issues == null) {
            return new FixIndex(tableRenames, fieldFixes, missingRequired);
        }

        for (LintIssue issue : issues) {
            String ruleCode = issue.getRuleCode();
            String tableLower = lower(issue.getTableName());

            // 表名重命名:只认 table_naming_snake_case 的 replacement
            if ("table_naming_snake_case".equals(ruleCode)
                    && issue.getReplacement() != null
                    && issue.getTableName() != null) {
                tableRenames.put(tableLower, issue.getReplacement());
                continue;
            }

            // 字段级修复:snake_case / forbidden / recommended 都给 replacement
            if (isFieldRenameRule(ruleCode)
                    && issue.getReplacement() != null
                    && issue.getColumnName() != null) {
                String key = tableLower + "|" + issue.getColumnName().toLowerCase();
                FieldFix existing = fieldFixes.get(key);
                FieldFix candidate = new FieldFix(issue.getReplacement(), issue.getConfidence());
                // 同一字段取 confidence 最高的修复建议
                if (existing == null || candidate.higherThan(existing)) {
                    fieldFixes.put(key, candidate);
                }
                continue;
            }

            // 必备列缺失:用 after 作为完整列定义片段
            if ("required_columns".equals(ruleCode)
                    && issue.getReplacement() != null
                    && issue.getAfter() != null) {
                missingRequired
                        .computeIfAbsent(tableLower, k -> new LinkedHashMap<>())
                        .put(issue.getReplacement().toLowerCase(), issue.getAfter());
            }
        }
        return new FixIndex(tableRenames, fieldFixes, missingRequired);
    }

    private boolean isFieldRenameRule(String ruleCode) {
        return "field_naming_snake_case".equals(ruleCode)
                || "forbidden_field_name".equals(ruleCode)
                || "recommended_field_name".equals(ruleCode);
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
        String tableLower = originalName.toLowerCase();
        String targetName = fixIndex.tableRenames().getOrDefault(tableLower, originalName);
        if (!SAFE_IDENTIFIER.matcher(targetName).matches()) {
            // 重建出来的表名必须仍是合法标识符,否则放弃
            return null;
        }

        // 收集最终列(保留原顺序),并记录每个最终列的注释
        List<ColumnDef> originalColumns = table.getColumns() != null ? table.getColumns() : List.of();
        List<ColumnDef> finalColumns = new ArrayList<>();
        Map<String, String> finalColumnComments = new LinkedHashMap<>();
        Set<String> finalColumnNamesLower = new HashSet<>();

        for (ColumnDef col : originalColumns) {
            if (col.getName() == null) {
                return null;
            }
            String fixKey = tableLower + "|" + col.getName().toLowerCase();
            FieldFix fix = fixIndex.fieldFixes().get(fixKey);
            String finalName = fix != null ? fix.replacement() : col.getName();
            if (!SAFE_IDENTIFIER.matcher(finalName).matches()) {
                return null;
            }
            // 防止重命名后与已有列冲突(例如两个字段都建议改成同名),冲突则放弃该表重建
            String finalNameLower = finalName.toLowerCase();
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

        // 补必备列:只补当前不存在的(用最终列名集合判断)
        Map<String, String> missing = fixIndex.missingRequired().get(tableLower);
        if (missing != null) {
            for (Map.Entry<String, String> entry : missing.entrySet()) {
                String requiredNameLower = entry.getKey();
                if (finalColumnNamesLower.contains(requiredNameLower)) {
                    continue;
                }
                ColumnDef requiredCol = parseColumnSnippet(entry.getValue());
                if (requiredCol == null) {
                    // after 片段不是确定的“列名 类型”格式,放弃重建
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
            String comment = columnComments.get(col.getName().toLowerCase());
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

    /** 单个字段的修复建议(replacement + 置信度) */
    private record FieldFix(String replacement, Integer confidence) {
        boolean higherThan(FieldFix other) {
            int mine = confidence == null ? 0 : confidence;
            int theirs = other.confidence == null ? 0 : other.confidence;
            return mine > theirs;
        }
    }

    /** 预索引的修复集合 */
    private record FixIndex(
            Map<String, String> tableRenames,
            Map<String, FieldFix> fieldFixes,
            Map<String, Map<String, String>> missingRequired
    ) {
    }
}
