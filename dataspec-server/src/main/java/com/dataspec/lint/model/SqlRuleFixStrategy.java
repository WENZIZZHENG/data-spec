package com.dataspec.lint.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 单条规则在当前 fixedSql 策略下的修复计划快照。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SqlRuleFixStrategy {

    /** 本次调试沿用的 fixedSql 策略；为空表示使用系统默认策略。 */
    private FixPolicy fixPolicy;

    /** 是否为 dry-run 修复预览。 */
    private Boolean fixDryRun;

    /** 该规则产生的修复变更汇总。 */
    private FixPlanSummary fixSummary;

    /** 该规则产生的修复变更列表；没有确定性修复时为空列表。 */
    private List<FixChange> changes;

    /** 针对该规则修复结果的后续建议动作。 */
    private List<String> nextActions;
}
