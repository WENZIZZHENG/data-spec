package com.dataspec.lint.rules;

import com.dataspec.lint.model.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 字段命名必须是 snake_case
 */
@Component
public class FieldNamingSnakeCaseRule implements LintRule {

    private static final Pattern SNAKE_CASE = Pattern.compile("^[a-z][a-z0-9]*(_[a-z0-9]+)*$");

    @Override
    public String getCode() {
        return "field_naming_snake_case";
    }

    @Override
    public String getName() {
        return "字段命名 snake_case 校验";
    }

    @Override
    public List<LintIssue> check(RuleContext context) {
        List<LintIssue> issues = new ArrayList<>();
        for (TableDef table : context.getTables()) {
            for (ColumnDef col : table.getColumns()) {
                if (!SNAKE_CASE.matcher(col.getName()).matches()) {
                    issues.add(LintIssue.builder()
                            .severity(Severity.ERROR)
                            .ruleCode(getCode())
                            .ruleName(getName())
                            .tableName(table.getName())
                            .columnName(col.getName())
                            .message(String.format("字段 '%s.%s' 不符合 snake_case 命名规范",
                                    table.getName(), col.getName()))
                            .build());
                }
            }
        }
        return issues;
    }
}
