package com.dataspec.fieldquality.model;

import lombok.Data;

/**
 * 字段质量报告汇总。
 */
@Data
public class FieldQualitySummary {

    private int totalFieldCount;
    private int averageScore;
    private int goodCount;
    private int warningCount;
    private int poorCount;
    private int lowQualityCount;
    private int errorIssueCount;
    private int warningIssueCount;
    private int suggestionIssueCount;
}
