package com.dataspec.explaintrace.model;

/**
 * AI 输出的轻量证据引用。
 *
 * @param sourceType      证据来源类型，例如 FIELD、TEMPLATE、REQUIREMENT_DRAFT
 * @param sourceId        来源对象 ID；临时规则或候选可为空
 * @param snapshotVersion 来源快照版本；当前来源没有稳定快照时可为空
 * @param matchReason     可读命中原因，供 AI 和前端解释推荐依据
 * @param confidence      0-100 的确定性置信度；来源无法评分时可为空
 * @param ruleCode        规则或生成器代码；非规则来源可为空
 * @param docsRef         文档锚点或说明入口，帮助复盘该证据的含义
 */
public record ExplainTrace(
        String sourceType,
        Long sourceId,
        String snapshotVersion,
        String matchReason,
        Integer confidence,
        String ruleCode,
        String docsRef
) {
}
