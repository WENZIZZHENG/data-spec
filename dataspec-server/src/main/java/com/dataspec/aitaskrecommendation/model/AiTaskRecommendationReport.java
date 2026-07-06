package com.dataspec.aitaskrecommendation.model;

import java.util.List;

/**
 * 项目级 AI 任务推荐队列响应。
 *
 * @param projectId 推荐队列所属项目 ID。
 * @param summary 推荐任务摘要。
 * @param items 按优先级排序的推荐任务卡列表。
 */
public record AiTaskRecommendationReport(
        Long projectId,
        AiTaskRecommendationSummary summary,
        List<AiTaskRecommendationItem> items
) {
}
