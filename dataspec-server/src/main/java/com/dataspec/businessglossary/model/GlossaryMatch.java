package com.dataspec.businessglossary.model;

import com.dataspec.querynormalization.model.QueryTokenResolutionStatus;

import java.util.List;
import java.util.Set;

/**
 * 业务术语命中结果，供字段推荐和字段检索复用。
 *
 * @param glossaryId         主要匹配条目 ID；多条折叠时为稳定排序后的第一项
 * @param term               主要 canonical 术语
 * @param matchedToken       实际命中的术语、同义词、词根、缩写或禁用词
 * @param matchType          TERM、SYNONYM、ROOT、ABBREVIATION 或 DISABLED
 * @param score              确定性评分；歧义和禁用匹配固定为 0
 * @param canonicalFieldId   唯一 canonical 字段 ID；歧义和禁用匹配为空
 * @param canonicalFieldName 唯一 canonical 字段名；未绑定、歧义和禁用匹配为空
 * @param exampleFields      相关示例字段名的有界集合
 * @param disabledTerm       是否命中禁用词；保留该字段兼容既有评分调用
 * @param reason             经脱敏和限长的匹配原因
 * @param resolutionStatus   词典解析状态
 * @param glossaryIds        支撑该结果的 glossary 条目 ID；数量有界
 * @param startOffset        匹配在规范化 compact query 中的 UTF-16 起始偏移；旧调用未知时为 -1
 * @param endOffset          匹配在规范化 compact query 中的 UTF-16 结束偏移；旧调用未知时为 -1
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
        String reason,
        QueryTokenResolutionStatus resolutionStatus,
        List<Long> glossaryIds,
        int startOffset,
        int endOffset
) {

    /**
     * 兼容既有完整状态构造调用；没有位置数据时保留 -1，由 normalization 使用确定性文本回退。
     */
    public GlossaryMatch(
            Long glossaryId,
            String term,
            String matchedToken,
            String matchType,
            int score,
            Long canonicalFieldId,
            String canonicalFieldName,
            Set<String> exampleFields,
            boolean disabledTerm,
            String reason,
            QueryTokenResolutionStatus resolutionStatus,
            List<Long> glossaryIds
    ) {
        this(
                glossaryId,
                term,
                matchedToken,
                matchType,
                score,
                canonicalFieldId,
                canonicalFieldName,
                exampleFields,
                disabledTerm,
                reason,
                resolutionStatus,
                glossaryIds,
                -1,
                -1);
    }

    /**
     * 兼容既有单条匹配构造调用；新确定性匹配由完整构造器表达多来源状态。
     */
    public GlossaryMatch(
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
        this(
                glossaryId,
                term,
                matchedToken,
                matchType,
                score,
                canonicalFieldId,
                canonicalFieldName,
                exampleFields,
                disabledTerm,
                reason,
                disabledTerm ? QueryTokenResolutionStatus.DISABLED : QueryTokenResolutionStatus.RESOLVED,
                glossaryId == null ? List.of() : List.of(glossaryId),
                -1,
                -1);
    }
}
