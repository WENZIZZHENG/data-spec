package com.dataspec.fieldprovenance.model;

import java.util.List;

/**
 * 单个标准字段的来源可信度与 AI 置信度摘要。
 *
 * @param fieldId 标准字段 ID。
 * @param name 字段英文名或技术名。
 * @param displayName 字段面向业务的显示名称，可为空。
 * @param status 字段生命周期状态，如 enabled、draft、deprecated、disabled。
 * @param primarySourceType 当前最主要的来源类型，优先取字段来源记录，其次取候选来源。
 * @param sourceRefs 脱敏后的来源引用摘要，最多用于解释证据来源，不包含 raw metadata 或 raw evidence。
 * @param sourceEvidenceCount 字段来源记录数量。
 * @param candidateEvidenceCount 已采纳、已合并、待处理或暂缓候选中与该字段相关的证据数量。
 * @param evidenceCount 来源证据与候选证据总数。
 * @param qualityScore 字段质量评分，缺少质量报告时为空。
 * @param qualityLevel 字段质量分档名称，缺少质量报告时为空。
 * @param aiConfidence 面向 AI 的 0 到 100 置信度分数。
 * @param confidenceLevel 面向用户和 AI 的可信度分档。
 * @param recommendedUse 建议 AI 如何使用该字段。
 * @param warnings 需要人工复核的原因列表。
 */
public record FieldProvenanceConfidenceItem(
        Long fieldId,
        String name,
        String displayName,
        String status,
        String primarySourceType,
        List<String> sourceRefs,
        int sourceEvidenceCount,
        int candidateEvidenceCount,
        int evidenceCount,
        Integer qualityScore,
        String qualityLevel,
        int aiConfidence,
        FieldProvenanceConfidenceLevel confidenceLevel,
        String recommendedUse,
        List<String> warnings
) {
}
