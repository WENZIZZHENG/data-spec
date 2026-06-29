package com.dataspec.businessglossary.model;

import java.util.List;

/**
 * AI Context 中导出的精简术语条目。
 */
public record BusinessGlossaryContextItem(
        String term,
        List<String> synonyms,
        List<String> rootTerms,
        List<String> abbreviations,
        List<String> disabledTerms,
        String canonicalFieldName,
        String scopeType,
        String scopeValue,
        List<String> exampleFields
) {
}
