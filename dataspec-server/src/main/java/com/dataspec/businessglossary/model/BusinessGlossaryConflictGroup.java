package com.dataspec.businessglossary.model;

import java.util.List;

/**
 * 一组术语冲突。
 */
public record BusinessGlossaryConflictGroup(
        String type,
        String severity,
        String token,
        String message,
        List<BusinessGlossaryConflictEntry> entries,
        String nextAction
) {
}
