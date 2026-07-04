package com.dataspec.standardqualitygate.model;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 项目标准质量门禁配置 DTO。
 */
@Data
public class StandardQualityGateConfig {

    private Long projectId;
    private Boolean enabled;
    private Integer minCoverage;
    private Integer minAverageFieldScore;
    private Integer maxErrorIssues;
    private Integer maxNewUnmanagedFields;
    private Boolean requiredSensitiveMarking;
    private LocalDateTime updatedAt;
}
