package com.dataspec.lint.rules;

import com.dataspec.lint.model.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 字段注释缺失提醒
 */
@Component
public class CommentMissingRule implements LintRule {

    @Override
    public String getCode() {
        return "comment_missing";
    }

    @Override
    public String getName() {
        return "字段注释缺失提醒";
    }

    @Override
    public List<LintIssue> check(RuleContext context) {
        List<LintIssue> issues = new ArrayList<>();
        for (TableDef table : context.getTables()) {
            // 检查表注释
            if (table.getComment() == null || table.getComment().isBlank()) {
                issues.add(LintIssue.builder()
                        .severity(Severity.SUGGESTION)
                        .ruleCode(getCode())
                        .ruleName(getName())
                        .tableName(table.getName())
                        .message(String.format("表 '%s' 缺少注释", table.getName()))
                        .build());
            }

            // 检查字段注释
            for (ColumnDef col : table.getColumns()) {
                if (col.getComment() == null || col.getComment().isBlank()) {
                    issues.add(LintIssue.builder()
                            .severity(Severity.SUGGESTION)
                            .ruleCode(getCode())
                            .ruleName(getName())
                            .tableName(table.getName())
                            .columnName(col.getName())
                            .message(String.format("字段 '%s.%s' 缺少注释",
                                    table.getName(), col.getName()))
                            .build());
                }
            }
        }
        return issues;
    }
}
