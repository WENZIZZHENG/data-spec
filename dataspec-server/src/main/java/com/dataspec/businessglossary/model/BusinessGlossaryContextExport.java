package com.dataspec.businessglossary.model;

import java.util.List;

/**
 * AI Context 术语导出结果。
 */
public record BusinessGlossaryContextExport(
        List<BusinessGlossaryContextItem> items,
        boolean truncated,
        int totalCount,
        int returnedCount
) {
    public static BusinessGlossaryContextExport empty() {
        return new BusinessGlossaryContextExport(List.of(), false, 0, 0);
    }
}
