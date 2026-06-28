package com.dataspec.lint.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 单条 fixedSql 变更或跳过解释。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FixChange {

    private FixChangeStatus status;
    private String reasonCode;
    private String ruleCode;
    private String ruleName;
    private FixRiskLevel riskLevel;
    private FixChangeType changeType;
    private String tableName;
    private String columnName;
    private String before;
    private String after;
    private String explain;
    private Integer confidence;
    private Integer sourceStart;
    private Integer sourceEnd;
}
