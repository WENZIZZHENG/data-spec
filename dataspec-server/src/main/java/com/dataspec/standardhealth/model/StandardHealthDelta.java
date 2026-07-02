package com.dataspec.standardhealth.model;

import lombok.Data;

/**
 * 与某个历史基线快照的指标变化。
 */
@Data
public class StandardHealthDelta {

    private Long baselineSnapshotId;

    private Integer days;

    private Integer qualityAverageScoreDelta;

    private Integer lowQualityFieldCountDelta;

    private Double coverageRateDelta;

    private Integer unmanagedFieldCountDelta;

    private String summary;
}
