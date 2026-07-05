package com.dataspec.lint.model;

/**
 * SQL 规则调试 trace 的稳定状态枚举。
 */
public enum SqlRuleMatchStatus {
    /** 规则已执行并产生命中的 lint issue。 */
    MATCHED,

    /** 规则已执行但当前 SQL 和参数快照下没有命中。 */
    NO_MATCH,

    /** 规则因项目配置禁用而未执行。 */
    DISABLED,

    /** SQL 未解析出可检查对象，规则未执行。 */
    UNPARSED,

    /** 规则执行异常，已转换为可读调试信息。 */
    ERROR
}
