package com.dataspec.lint.rules;

import com.dataspec.lint.model.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 禁用字段名校验
 * <p>
 * 默认禁用: uid, create_time, update_time, del_flag
 * 可通过 ruleParams.forbiddenNames 覆盖
 */
@Component
public class ForbiddenFieldNameRule implements LintRule {

    private static final Set<String> DEFAULT_FORBIDDEN = Set.of(
            "uid", "create_time", "update_time", "del_flag",
            "ctime", "mtime", "is_del", "tmp", "test", "flag1", "type1"
    );
    private static final java.util.Map<String, String> DEFAULT_REPLACEMENTS = java.util.Map.of(
            "uid", "user_id",
            "create_time", "created_at",
            "update_time", "updated_at",
            "del_flag", "is_deleted",
            "ctime", "created_at",
            "mtime", "updated_at",
            "is_del", "is_deleted"
    );

    @Override
    public String getCode() {
        return "forbidden_field_name";
    }

    @Override
    public String getName() {
        return "禁用字段名校验";
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<LintIssue> check(RuleContext context) {
        Set<String> forbidden = DEFAULT_FORBIDDEN;
        if (context.getRuleParams() != null && context.getRuleParams().containsKey("forbiddenNames")) {
            Object names = context.getRuleParams().get("forbiddenNames");
            if (names instanceof List<?> list) {
                forbidden = Set.copyOf((List<String>) list);
            }
        }

        List<LintIssue> issues = new ArrayList<>();
        for (TableDef table : context.getTables()) {
            for (ColumnDef col : table.getColumns()) {
                if (forbidden.contains(col.getName().toLowerCase())) {
                    String lowerName = col.getName().toLowerCase();
                    String replacement = DEFAULT_REPLACEMENTS.get(lowerName);
                    issues.add(LintIssue.builder()
                            .severity(Severity.ERROR)
                            .ruleCode(getCode())
                            .ruleName(getName())
                            .tableName(table.getName())
                            .columnName(col.getName())
                            .message(String.format("字段 '%s.%s' 是禁用字段名，请替换为规范命名",
                                    table.getName(), col.getName()))
                            .suggestion(replacement != null
                                    ? String.format("将禁用字段名 '%s' 替换为 '%s'", col.getName(), replacement)
                                    : String.format("将禁用字段名 '%s' 替换为具备业务语义的规范字段名", col.getName()))
                            .replacement(replacement)
                            .before(col.getName())
                            .after(replacement)
                            .confidence(replacement != null ? 90 : 60)
                            .build());
                }
            }
        }
        return issues;
    }
}
