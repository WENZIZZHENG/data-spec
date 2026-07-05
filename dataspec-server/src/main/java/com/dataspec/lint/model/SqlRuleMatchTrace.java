package com.dataspec.lint.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 单条 SQL 规则调试事件，描述规则为什么命中、未命中或被跳过。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SqlRuleMatchTrace {

    /** 稳定状态，供 AI 和前端按状态分组展示。 */
    private SqlRuleMatchStatus status;

    /** 人类可读解释，说明命中、未命中、禁用或异常原因。 */
    private String message;

    /** 命中 issue 的严重级别；未命中或禁用时可为空。 */
    private Severity severity;

    /** 命中 issue 的原始问题描述；未命中或禁用时为空。 */
    private String issueMessage;

    /** 本次 trace 对应的表名；没有具体表时为空。 */
    private String tableName;

    /** 本次 trace 对应的字段名；表级问题或无具体字段时为空。 */
    private String columnName;

    /** 本次 trace 对应的源 SQL 范围；无法定位时为空。 */
    private SqlRuleSourceRange sourceRange;

    /** 当前 fixedSql 策略下的修复状态；未参与修复策略时为空。 */
    private FixChangeStatus fixStatus;

    /** fixedSql 跳过原因编码；未跳过时为空。 */
    private String fixReasonCode;

    /** 命中规则豁免 ID；未被豁免时为空。 */
    private Long suppressionId;
}
