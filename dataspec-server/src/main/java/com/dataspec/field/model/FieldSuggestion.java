package com.dataspec.field.model;

import com.dataspec.explaintrace.model.ExplainTrace;
import com.dataspec.field.entity.Field;

import java.util.List;

/**
 * 字段推荐结果。
 *
 * @param field           命中的已有标准字段；fallback 建议时为空
 * @param score           确定性匹配分数，数值越高越推荐
 * @param matchReason     命中原因，供 AI 或前端解释为什么推荐
 * @param recommendedName 推荐字段名；已有字段命中时等于标准字段名
 * @param existing        是否来自已有标准字段库
 * @param evidence        结构化证据链，供 AI 稳定读取来源和置信度
 */
public record FieldSuggestion(
        Field field,
        int score,
        String matchReason,
        String recommendedName,
        boolean existing,
        List<ExplainTrace> evidence
) {
    public FieldSuggestion(Field field, int score, String matchReason, String recommendedName, boolean existing) {
        this(field, score, matchReason, recommendedName, existing, List.of());
    }
}
