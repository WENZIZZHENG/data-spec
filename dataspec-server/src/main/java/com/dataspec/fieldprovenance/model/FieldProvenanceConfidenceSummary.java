package com.dataspec.fieldprovenance.model;

/**
 * 项目级字段来源可信度聚合计数。
 *
 * @param totalFieldCount 项目内参与本次聚合的标准字段总数。
 * @param verifiedCount `VERIFIED` 分档字段数。
 * @param reviewCount `REVIEW` 分档字段数。
 * @param lowCount `LOW` 分档字段数。
 * @param unknownCount `UNKNOWN` 分档字段数。
 * @param fieldsWithSourceEvidence 至少存在一条字段来源记录的字段数。
 * @param fieldsWithCandidateEvidence 至少存在一条相关标准候选证据的字段数。
 * @param fieldsWithWarnings 存在复核提醒的字段数。
 */
public record FieldProvenanceConfidenceSummary(
        int totalFieldCount,
        int verifiedCount,
        int reviewCount,
        int lowCount,
        int unknownCount,
        int fieldsWithSourceEvidence,
        int fieldsWithCandidateEvidence,
        int fieldsWithWarnings
) {
}
