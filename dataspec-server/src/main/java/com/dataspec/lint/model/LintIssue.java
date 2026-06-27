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

    /** 结构化修复建议，供 AI 或前端直接展示 */
    private String suggestion;

    /** 推荐替换值，如字段名、表名或数据类型 */
    private String replacement;

    /** 修复前的值 */
    private String before;

    /** 修复后的值或建议补充片段 */
    private String after;

    /** 建议置信度，范围 0-100 */
    private Integer confidence;

    /** 1-based 源 SQL 行号；无法定位时为空 */
    private Integer line;

    /** 1-based 源 SQL 列号；无法定位时为空 */
    private Integer column;

    /** 1-based 源 SQL 结束行号；无法定位时为空 */
    private Integer lineEnd;

    /** 1-based 源 SQL 结束列号（不含）；无法定位时为空 */
    private Integer columnEnd;

    /** 0-based 源 SQL 起始偏移；无法定位时为空 */
    private Integer sourceStart;

    /** 0-based 源 SQL 结束偏移（不含）；无法定位时为空 */
    private Integer sourceEnd;

    /** 定位类型，如 table、column、comment_column；无法定位时为空 */
    private String locationKind;

    /** 是否被项目规则豁免抑制；被抑制的问题保留展示但不计入 active 统计 */
    private Boolean suppressed;

    /** 命中的规则豁免 ID */
    private Long suppressionId;

    /** 命中的规则豁免原因 */
    private String suppressionReason;
}
