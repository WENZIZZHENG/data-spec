package com.dataspec.aitaskrecommendation.model;

/**
 * AI 推荐任务队列摘要。
 *
 * @param totalTaskCount 推荐任务总数。
 * @param highPriorityCount HIGH 优先级任务数。
 * @param mediumPriorityCount MEDIUM 优先级任务数。
 * @param lowPriorityCount LOW 优先级任务数。
 * @param evidenceSourceCount 本次推荐引用的不同证据来源数量。
 */
public record AiTaskRecommendationSummary(
        int totalTaskCount,
        int highPriorityCount,
        int mediumPriorityCount,
        int lowPriorityCount,
        int evidenceSourceCount
) {
}
