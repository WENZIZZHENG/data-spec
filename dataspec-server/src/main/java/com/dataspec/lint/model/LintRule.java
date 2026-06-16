package com.dataspec.lint.model;

import java.util.List;

/**
 * 规则接口 —— 每条规则实现此接口，Spring 自动注册
 */
public interface LintRule {

    /** 规则编码，对应 RuleConfig.ruleCode */
    String getCode();

    /** 规则名称 */
    String getName();

    /** 执行检查，返回发现的问题列表 */
    List<LintIssue> check(RuleContext context);
}
