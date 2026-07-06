package com.dataspec.fieldprovenance.model;

import java.util.List;

/**
 * 项目字段来源可信度报告。
 *
 * @param projectId 本次报告所属项目 ID。
 * @param summary 项目级来源可信度汇总。
 * @param fields 字段级来源可信度列表，字段越需要复核通常越靠前。
 */
public record FieldProvenanceConfidenceReport(
        Long projectId,
        FieldProvenanceConfidenceSummary summary,
        List<FieldProvenanceConfidenceItem> fields
) {
}
