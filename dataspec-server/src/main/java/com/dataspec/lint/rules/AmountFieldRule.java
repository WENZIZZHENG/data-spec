package com.dataspec.lint.rules;

import com.dataspec.lint.model.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 金额字段类型校验
 * <p>
 * 包含 amount 关键字的字段应使用 bigint（存分）或 numeric（精确计算），
 * 不推荐使用 float / double / real / money。
 */
@Component
public class AmountFieldRule implements LintRule {

    private static final Set<String> BAD_TYPES = Set.of(
            "float", "double", "double precision", "real", "money", "float4", "float8"
    );

    @Override
    public String getCode() {
        return "amount_field_type";
    }

    @Override
    public String getName() {
        return "金额字段类型校验";
    }

    @Override
    public List<LintIssue> check(RuleContext context) {
        List<LintIssue> issues = new ArrayList<>();
        for (TableDef table : context.getTables()) {
            for (ColumnDef col : table.getColumns()) {
                if (col.getName().toLowerCase().contains("amount")) {
                    String dt = col.getDataType().toLowerCase();
                    if (BAD_TYPES.contains(dt)) {
                        issues.add(LintIssue.builder()
                                .severity(Severity.WARNING)
                                .ruleCode(getCode())
                                .ruleName(getName())
                                .tableName(table.getName())
                                .columnName(col.getName())
                                .message(String.format(
                                        "金额字段 '%s.%s' 使用了 '%s' 类型，建议使用 bigint（存分）或 numeric",
                                        table.getName(), col.getName(), col.getDataType()))
                                .build());
                    }
                }
            }
        }
        return issues;
    }
}
