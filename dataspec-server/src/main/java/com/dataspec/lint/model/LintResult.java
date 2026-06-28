package com.dataspec.lint.model;

import com.dataspec.dialect.model.DialectDiagnostic;
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

    /** 本次 fixedSql 生成使用的有效策略 */
    private FixPolicy fixPolicy;

    /** dry-run 模式下为 true，表示 fixedSql 只是预览候选 */
    private Boolean fixDryRun;

    /** 当前策略下识别到的 applied/planned/skipped 修复变更与解释 */
    private List<FixChange> fixChanges;

    /** 跳过或不可自动修复项的解释，便于 AI 直接读取 */
    private List<FixChange> fixExplanations;

    /** fixedSql 策略执行摘要 */
    private FixPlanSummary fixSummary;

    /** 应用 fixedSql 前后的建议动作 */
    private List<String> fixNextActions;

    /** 方言识别、支持矩阵和降级提示，供 AI/CLI/前端判断结果边界 */
    private List<DialectDiagnostic> dialectDiagnostics;

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
