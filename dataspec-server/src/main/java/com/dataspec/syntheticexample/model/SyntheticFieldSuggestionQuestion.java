package com.dataspec.syntheticexample.model;

import java.util.List;

/**
 * 字段推荐评测问题，用于验证自然语言描述能否命中预期标准字段。
 *
 * @param id 稳定问题 id。
 * @param question 自然语言业务问题。
 * @param expectedFieldNames 期望推荐命中的字段名。
 * @param reviewHint 人工复核或 AI 评测提示。
 */
public record SyntheticFieldSuggestionQuestion(
        String id,
        String question,
        List<String> expectedFieldNames,
        String reviewHint
) {
}
