package com.dataspec.fieldprovenance.model;

/**
 * 标准字段来源可信度分档，供用户和 AI 判断字段能否作为强标准直接使用。
 */
public enum FieldProvenanceConfidenceLevel {
    /** 来源证据、候选决策和质量评分都较强，可作为 AI 首选标准字段。 */
    VERIFIED,
    /** 有一定证据但仍建议生成前复核，适合作为 AI 推荐候选。 */
    REVIEW,
    /** 证据较弱或存在明显提醒，只能作为候选参考。 */
    LOW,
    /** 缺少足够证据，需人工确认后再进入强约束。 */
    UNKNOWN
}
