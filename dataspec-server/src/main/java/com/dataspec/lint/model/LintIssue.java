package com.dataspec.lint.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 校验发现的问题
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LintIssue {

    /** 严重级别 */
    private Severity severity;

    /** 规则编码 */
    private String ruleCode;

    /** 规则名称 */
    private String ruleName;

    /** 问题描述 */
    private String message;

    /** 涉及的表名 */
    private String tableName;

    /** 涉及的字段名（可选） */
    private String columnName;
}
