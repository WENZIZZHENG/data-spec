package com.dataspec.aicontext.model;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * AI Context 预算计划请求。
 *
 * <p>请求只用于只读 preflight；服务端不得因为本请求创建 zip、缓存文件或修改项目状态。</p>
 *
 * @param projectId 项目 ID，必填，用于读取当前项目标准元数据
 * @param tokenBudget 调用方可接受的 token 预算，必须为正数，单位为保守估算 token
 * @param taskType 可选任务类型，用于匹配 AI profile 默认 scope
 * @param profileId 可选 AI profile ID，优先于 taskType 匹配 profile
 * @param scope 可选裁剪范围，沿用 AI Context scoped export 的 scope 语义
 * @param query 可选检索词，用于按字段名、显示名、注释、标签、分类等文本裁剪
 * @param status 可选字段状态过滤，例如 enabled 或 deprecated
 * @param limit 可选返回字段上限，缺省时不主动限制字段数量
 * @param targetTable 可选目标表提示；query 缺失时可作为裁剪提示，响应中只返回脱敏摘要
 * @param targetFile 可选目标文件提示；query 和 targetTable 缺失时可作为裁剪提示，响应中只返回脱敏摘要
 */
public record AiContextBudgetPlanRequest(
        @NotNull(message = "项目ID不能为空") Long projectId,
        @NotNull(message = "tokenBudget 不能为空")
        @Min(value = 1, message = "tokenBudget 必须大于 0") Integer tokenBudget,
        String taskType,
        String profileId,
        String scope,
        String query,
        String status,
        Integer limit,
        String targetTable,
        String targetFile
) {
}
