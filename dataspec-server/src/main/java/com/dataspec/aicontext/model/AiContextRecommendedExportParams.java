package com.dataspec.aicontext.model;

/**
 * AI Context planner 推荐的导出参数。
 *
 * <p>该对象只表达建议，前端和 CLI 不得据此静默覆盖用户已输入的导出参数。</p>
 *
 * @param scope 推荐使用的 scoped export 范围，缺省完整导出时为 {@code all}
 * @param query 推荐的检索词；可能来自 query、targetTable 或 targetFile，输出前必须已脱敏
 * @param status 推荐的字段状态过滤，缺省表示不限制状态
 * @param limit 推荐的字段上限，缺省表示不主动截断
 * @param profileId 推荐沿用的 AI profile ID，缺省表示不指定 profile
 * @param taskType 推荐沿用的 AI taskType，缺省表示不指定任务类型
 */
public record AiContextRecommendedExportParams(
        String scope,
        String query,
        String status,
        Integer limit,
        String profileId,
        String taskType
) {
}
