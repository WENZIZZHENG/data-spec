package com.dataspec.aicontext.model;

import java.util.List;

/**
 * AI Context 预算计划响应。
 *
 * <p>响应只包含计数、估算、artifact 名称、风险和建议动作，不包含完整字段目录、规则正文、连接串或凭据。</p>
 *
 * @param kind 响应类型标识，固定为 dataspec-ai-context-budget-plan
 * @param schemaVersion 响应 schema 版本
 * @param projectId 项目 ID
 * @param request 脱敏后的请求摘要和 scope 命中计数
 * @param estimation token 估算摘要
 * @param selectedArtifacts 预算内建议保留的 artifact 列表
 * @param droppedArtifacts 因预算不足建议舍弃的 artifact 列表
 * @param qualityRisk 当前预算对 AI 任务质量的风险等级
 * @param fallbackSteps 预算不足或 scope 弱命中时的降级步骤
 * @param recommendedExportParams 建议导出参数，仅供显式应用
 * @param diagnostics 诊断信息，不包含敏感原文
 * @param recommendedNextActions 推荐下一步动作，不包含敏感原文
 */
public record AiContextBudgetPlan(
        String kind,
        int schemaVersion,
        Long projectId,
        AiContextBudgetRequestEcho request,
        AiContextBudgetEstimation estimation,
        List<AiContextBudgetArtifact> selectedArtifacts,
        List<AiContextBudgetArtifact> droppedArtifacts,
        AiContextBudgetQualityRisk qualityRisk,
        List<String> fallbackSteps,
        AiContextRecommendedExportParams recommendedExportParams,
        List<String> diagnostics,
        List<String> recommendedNextActions
) {
}
