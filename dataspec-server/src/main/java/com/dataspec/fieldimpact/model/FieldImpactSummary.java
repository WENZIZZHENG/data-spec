package com.dataspec.fieldimpact.model;

import lombok.Data;

/**
 * 字段影响报告汇总。
 */
@Data
public class FieldImpactSummary {

    private int totalImpactCount;
    private int templateImpactCount;
    private int importSourceImpactCount;
    private int sqlCheckImpactCount;
    private int snapshotImpactCount;
    private int codeSetImpactCount;
    private int warningCount;
}
