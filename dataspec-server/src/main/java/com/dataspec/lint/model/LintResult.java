package com.dataspec.lint.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * SQL 校验结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LintResult {

    /** 解析到的表定义 */
    private List<TableDef> tables;

    /** 发现的问题 */
    private List<LintIssue> issues;

    /** 统计 */
    private int errorCount;
    private int warningCount;
    private int suggestionCount;
    private int suppressedCount;

    /** 基于确定性修复建议重建的修正 SQL,无法重建时为 null */
    private String fixedSql;

    /** 原 SQL 与修正 SQL 的 unified diff,无差异时为 null */
    private String fixedSqlDiff;

    public static LintResult of(List<TableDef> tables, List<LintIssue> issues) {
        long errors = issues.stream().filter(LintResult::isActive).filter(i -> i.getSeverity() == Severity.ERROR).count();
        long warnings = issues.stream().filter(LintResult::isActive).filter(i -> i.getSeverity() == Severity.WARNING).count();
        long suggestions = issues.stream().filter(LintResult::isActive).filter(i -> i.getSeverity() == Severity.SUGGESTION).count();
        long suppressed = issues.stream().filter(i -> Boolean.TRUE.equals(i.getSuppressed())).count();

        return LintResult.builder()
                .tables(tables)
                .issues(issues)
                .errorCount((int) errors)
                .warningCount((int) warnings)
                .suggestionCount((int) suggestions)
                .suppressedCount((int) suppressed)
                .build();
    }

    private static boolean isActive(LintIssue issue) {
        return !Boolean.TRUE.equals(issue.getSuppressed());
    }
}
