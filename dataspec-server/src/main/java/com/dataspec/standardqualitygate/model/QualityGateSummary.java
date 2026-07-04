package com.dataspec.standardqualitygate.model;

import lombok.Data;

/**
 * 质量门禁评估汇总。
 */
@Data
public class QualityGateSummary {

    private int totalChecks;
    private int passedChecks;
    private int failedChecks;
    private int warningChecks;
    private int skippedChecks;
}
