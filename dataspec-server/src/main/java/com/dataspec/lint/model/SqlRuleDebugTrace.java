package com.dataspec.lint.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 单条 SQL lint 规则的调试快照。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SqlRuleDebugTrace {

    /** 规则编码，对应 LintRule.getCode() 和 RuleConfig.ruleCode。 */
    private String ruleCode;

    /** 规则名称，供用户在调试面板中识别规则。 */
    private String ruleName;

    /** 该规则在当前项目配置下是否会执行。 */
    private boolean enabled;

    /** 当前配置或命中 issue 推导出的严重级别；没有配置且未命中时为空。 */
    private Severity severity;

    /** 规则参数快照；敏感键和值会被脱敏。 */
    private Map<String, Object> paramsSnapshot;

    /** 规则命中、未命中、禁用或异常的结构化解释。 */
    private List<SqlRuleMatchTrace> matchTrace;

    /** 该规则首个可定位命中的源 SQL 范围；没有命中或无法定位时为空。 */
    private SqlRuleSourceRange sourceRange;

    /** 该规则在当前 fixedSql 策略下的修复计划快照。 */
    private SqlRuleFixStrategy fixStrategy;

    /** 该规则命中的豁免状态与计数。 */
    private SqlRuleSuppressionStatus suppressionStatus;

    /** 补充说明，解释通用 trace 或兼容降级原因。 */
    private List<String> debugNotes;
}
