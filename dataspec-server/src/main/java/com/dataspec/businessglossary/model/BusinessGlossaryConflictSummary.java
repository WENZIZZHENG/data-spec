package com.dataspec.businessglossary.model;

/**
 * 术语冲突汇总。
 */
public record BusinessGlossaryConflictSummary(
        int conflictCount,
        int errorCount,
        int warningCount
) {
}
