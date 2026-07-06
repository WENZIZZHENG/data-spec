package com.dataspec.aicontext.model;

/**
 * AI Context 预算计划中的请求摘要。
 *
 * <p>该摘要用于让 CLI/前端确认 planner 实际采用的 scope 和裁剪输入；所有用户文本必须已脱敏且可能被截断。</p>
 *
 * @param projectId 项目 ID
 * @param tokenBudget 请求中的 token 预算
 * @param taskType 采用的任务类型，可能来自请求或 profile 默认值
 * @param profileId 采用的 profile ID，可能来自请求或 profile 默认值
 * @param scope 实际裁剪范围，不支持的 scope 会降级为 all
 * @param query 实际检索词摘要，可能来自 query、targetTable 或 targetFile
 * @param status 实际字段状态过滤
 * @param limit 实际字段上限
 * @param targetTable 目标表提示摘要，仅用于诊断和推荐
 * @param targetFile 目标文件提示摘要，仅用于诊断和推荐
 * @param totalFieldCount 项目字段总数
 * @param matchedFieldCount 裁剪条件命中的字段数
 * @param returnedFieldCount 应用于 limit 后返回给估算的字段数
 */
public record AiContextBudgetRequestEcho(
        Long projectId,
        int tokenBudget,
        String taskType,
        String profileId,
        String scope,
        String query,
        String status,
        Integer limit,
        String targetTable,
        String targetFile,
        int totalFieldCount,
        int matchedFieldCount,
        int returnedFieldCount
) {
}
