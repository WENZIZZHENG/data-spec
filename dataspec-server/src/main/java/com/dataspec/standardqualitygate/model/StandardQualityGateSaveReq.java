package com.dataspec.standardqualitygate.model;

import lombok.Data;

/**
 * 保存项目质量门禁阈值。
 */
@Data
public class StandardQualityGateSaveReq {

    private Long projectId;
    private Boolean enabled;
    private Integer minCoverage;
    private Integer minAverageFieldScore;
    private Integer maxErrorIssues;
    private Integer maxNewUnmanagedFields;
    private Boolean requiredSensitiveMarking;
}
