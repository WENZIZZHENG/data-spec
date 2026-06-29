package com.dataspec.businessglossary.model;

import java.util.Set;

/**
 * 业务术语命中结果，供字段推荐和字段检索复用。
 */
public record GlossaryMatch(
        Long glossaryId,
        String term,
        String matchedToken,
        String matchType,
        int score,
        Long canonicalFieldId,
        String canonicalFieldName,
        Set<String> exampleFields,
        boolean disabledTerm,
        String reason
) {
}
