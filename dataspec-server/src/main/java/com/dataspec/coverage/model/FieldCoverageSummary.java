package com.dataspec.coverage.model;

import lombok.Data;

/**
 * 字段覆盖率汇总。
 */
@Data
public class FieldCoverageSummary {

    private int tableCount;
    private int columnCount;
    private int coveredCount;
    private int unmanagedCount;
    private int missingCommentCount;
    private int possibleDuplicateCount;
    private double coverageRate;
}
