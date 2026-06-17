package com.dataspec.lint.rules;

import com.dataspec.lint.model.LintIssue;
import com.dataspec.lint.model.LintRule;
import com.dataspec.lint.model.RuleContext;
import com.dataspec.lint.model.Severity;
import com.dataspec.lint.model.TableDef;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 表名必须是 snake_case
 */
@Component
public class TableNameSnakeCaseRule implements LintRule {

    private static final Pattern SNAKE_CASE = Pattern.compile("^[a-z][a-z0-9]*(_[a-z0-9]+)*$");

    @Override
    public String getCode() {
        return "table_naming_snake_case";
    }

    @Override
    public String getName() {
        return "表名 snake_case 校验";
    }

    @Override
    public List<LintIssue> check(RuleContext context) {
        List<LintIssue> issues = new ArrayList<>();
        for (TableDef table : context.getTables()) {
            if (!SNAKE_CASE.matcher(table.getName()).matches()) {
                String replacement = RuleFixSupport.toSnakeCase(table.getName());
                issues.add(LintIssue.builder()
                        .severity(Severity.ERROR)
                        .ruleCode(getCode())
                        .ruleName(getName())
                        .tableName(table.getName())
                        .message(String.format("表 '%s' 不符合 snake_case 命名规范", table.getName()))
                        .suggestion(String.format("将表 '%s' 重命名为 '%s'", table.getName(), replacement))
                        .replacement(replacement)
                        .before(table.getName())
                        .after(replacement)
                        .confidence(90)
                        .build());
            }
        }
        return issues;
    }
}
