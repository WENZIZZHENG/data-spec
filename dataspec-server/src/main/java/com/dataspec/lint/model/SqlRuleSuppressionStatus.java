package com.dataspec.lint.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 单条规则在当前 SQL 中的豁免统计与命中说明。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SqlRuleSuppressionStatus {

    /** 未被豁免、仍计入 lint 统计的 issue 数量。 */
    private int activeIssueCount;

    /** 被项目规则豁免抑制的 issue 数量。 */
    private int suppressedIssueCount;

    /** 命中的规则豁免 ID 列表；没有豁免时为空列表。 */
    private List<Long> suppressionIds;

    /** 命中的规则豁免原因列表；没有豁免时为空列表。 */
    private List<String> suppressionReasons;

    /** 适合前端和 AI 直接展示的豁免状态摘要。 */
    private String summary;
}
