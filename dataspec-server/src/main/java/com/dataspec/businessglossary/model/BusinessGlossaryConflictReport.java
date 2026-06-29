package com.dataspec.businessglossary.model;

import java.util.List;

/**
 * 项目级术语冲突报告。
 */
public record BusinessGlossaryConflictReport(
        Long projectId,
        BusinessGlossaryConflictSummary summary,
        List<BusinessGlossaryConflictGroup> conflicts
) {
}
