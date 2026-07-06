package com.dataspec.syntheticexample.model;

import java.util.List;

/**
 * 标准问答案例，用于 Prompt 评测或 AI 回复校验。
 *
 * @param id 稳定问答 id。
 * @param question 用户或 AI 可能提出的标准问题。
 * @param expectedAnswerOutline 期望回答要点。
 * @param referencedFieldNames 回答应引用的标准字段名。
 * @param confidence 生成器对该案例的信心等级。
 * @param reviewHint 低置信或 fallback 时的人工复核提示。
 */
public record SyntheticStandardQaCase(
        String id,
        String question,
        String expectedAnswerOutline,
        List<String> referencedFieldNames,
        String confidence,
        String reviewHint
) {
}
