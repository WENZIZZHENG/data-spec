package com.dataspec.lint.rules;

import com.dataspec.lint.model.*;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 推荐字段名校验
 * <p>
 * 当检测到常见不规范命名时，给出推荐替换建议。
 * 例如: create_time → created_at, update_time → updated_at, user_name → username
 */
@Component
public class RecommendedFieldNameRule implements LintRule {

    private static final Map<String, String> DEFAULT_RECOMMENDATIONS = Map.of(
            "create_time", "created_at",
            "update_time", "updated_at",
            "delete_time", "deleted_at",
            "user_name", "username",
            "pass_word", "password",
            "is_delete", "is_deleted",
            "phone_number", "phone",
            "email_address", "email"
    );

    @Override
    public String getCode() {
        return "recommended_field_name";
    }

    @Override
    public String getName() {
        return "推荐字段名";
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<LintIssue> check(RuleContext context) {
        Map<String, String> recommendations = DEFAULT_RECOMMENDATIONS;
        if (context.getRuleParams() != null && context.getRuleParams().containsKey("recommendations")) {
            Object recs = context.getRuleParams().get("recommendations");
            if (recs instanceof Map<?, ?> map) {
                recommendations = (Map<String, String>) map;
            }
        }

        List<LintIssue> issues = new ArrayList<>();
        for (TableDef table : context.getTables()) {
            for (ColumnDef col : table.getColumns()) {
                String lower = col.getName().toLowerCase();
                if (recommendations.containsKey(lower)) {
                    String recommended = recommendations.get(lower);
                    issues.add(LintIssue.builder()
                            .severity(Severity.SUGGESTION)
                            .ruleCode(getCode())
                            .ruleName(getName())
                            .tableName(table.getName())
                            .columnName(col.getName())
                            .message(String.format("字段 '%s.%s' 建议改为 '%s'",
                                    table.getName(), col.getName(), recommended))
                            .suggestion(String.format("将字段 '%s' 替换为推荐字段名 '%s'", col.getName(), recommended))
                            .replacement(recommended)
                            .before(col.getName())
                            .after(recommended)
                            .confidence(95)
                            .build());
                }
            }
        }
        return issues;
    }
}
