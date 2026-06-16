package com.dataspec.lint.rules;

import com.dataspec.lint.model.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 业务表必须包含指定列
 * <p>
 * 默认必含: id, created_at, updated_at, is_deleted
 */
@Component
public class RequiredColumnsRule implements LintRule {

    private static final Set<String> DEFAULT_REQUIRED = Set.of(
            "id", "created_at", "updated_at", "is_deleted"
    );

    @Override
    public String getCode() {
        return "required_columns";
    }

    @Override
    public String getName() {
        return "必含列校验";
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<LintIssue> check(RuleContext context) {
        Set<String> required = DEFAULT_REQUIRED;
        if (context.getRuleParams() != null && context.getRuleParams().containsKey("requiredColumns")) {
            Object cols = context.getRuleParams().get("requiredColumns");
            if (cols instanceof List<?> list) {
                required = Set.copyOf((List<String>) list);
            }
        }

        List<LintIssue> issues = new ArrayList<>();
        for (TableDef table : context.getTables()) {
            Set<String> existingCols = table.getColumns().stream()
                    .map(c -> c.getName().toLowerCase())
                    .collect(Collectors.toSet());

            for (String req : required) {
                if (!existingCols.contains(req.toLowerCase())) {
                    issues.add(LintIssue.builder()
                            .severity(Severity.ERROR)
                            .ruleCode(getCode())
                            .ruleName(getName())
                            .tableName(table.getName())
                            .message(String.format("表 '%s' 缺少必含列 '%s'", table.getName(), req))
                            .build());
                }
            }
        }
        return issues;
    }
}
