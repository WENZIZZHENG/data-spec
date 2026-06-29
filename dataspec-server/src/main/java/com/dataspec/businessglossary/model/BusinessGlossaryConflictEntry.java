package com.dataspec.businessglossary.model;

/**
 * 术语冲突涉及的条目摘要。
 */
public record BusinessGlossaryConflictEntry(
        Long id,
        String term,
        Long canonicalFieldId,
        String canonicalFieldName
) {
}
