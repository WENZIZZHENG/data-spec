package com.dataspec.standardhealth.model;

import lombok.Data;

/**
 * 标准健康快照核心指标。
 */
@Data
public class StandardHealthMetrics {

    private Integer averageQualityScore;

    private Integer lowQualityFieldCount;

    private Integer totalFieldCount;

    private String coverageStatus;

    private Double coverageRate;

    private Integer unmanagedFieldCount;

    private Integer missingCommentCount;

    private Integer possibleDuplicateCount;

    private Integer ruleIssueCount;

    private Integer ruleExemptionCount;

    private Integer aiFeedbackSignalCount;

    private Integer pendingCandidateCount;

    private Integer adoptedCandidateCount;

    private Integer fixedSqlAvailableCount;
}
