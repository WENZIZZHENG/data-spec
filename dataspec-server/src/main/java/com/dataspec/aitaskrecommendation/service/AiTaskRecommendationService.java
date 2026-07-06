package com.dataspec.aitaskrecommendation.service;

import com.dataspec.aitaskrecommendation.model.AiTaskRecommendationReport;

/**
 * AI 下一步任务推荐只读聚合服务。
 */
public interface AiTaskRecommendationService {

    /**
     * 生成项目级推荐任务队列。
     *
     * @param projectId 项目 ID，必须有当前调用方访问权限。
     * @return 推荐任务队列，不包含 raw SQL、AI payload、候选原始 evidence 或凭据。
     */
    AiTaskRecommendationReport report(Long projectId);
}
