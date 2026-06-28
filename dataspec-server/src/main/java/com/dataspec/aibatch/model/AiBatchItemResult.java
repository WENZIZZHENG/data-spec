package com.dataspec.aibatch.model;

import com.dataspec.dialect.model.DialectDiagnostic;
import com.dataspec.lint.model.LintIssue;

import java.util.List;

/**
 * 批量交付包的单项 SQL lint 结果。
 */
public record AiBatchItemResult(
        String itemName,
        String filePath,
        String status,
        int errorCount,
        int warningCount,
        int suggestionCount,
        int suppressedCount,
        boolean fixedSqlAvailable,
        String fixedSql,
        String fixedSqlDiff,
        List<LintIssue> issues,
        List<DialectDiagnostic> dialectDiagnostics,
        Long sqlCheckRecordId,
        String errorMessage
) {
}
