package com.dataspec.aitaskrecommendation.model;

import java.util.List;

/**
 * AI 下一步推荐任务卡。
 *
 * @param taskType 稳定任务类型，用于 AI 或前端识别推荐来源和执行入口。
 * @param priority 推荐优先级，取值为 HIGH、MEDIUM 或 LOW。
 * @param title 人类可读任务标题。
 * @param reason 推荐该任务的脱敏原因摘要，不包含 raw SQL、AI payload 或凭据。
 * @param targetRoute 已有 DataSpec 页面或 API 入口。
 * @param recommendedCommand 可复制的安全命令或 API 调用模板，不包含 token、password 或本地路径。
 * @param evidenceRefs 支撑推荐的摘要证据引用，如计数、状态或来源能力标识。
 * @param completionCheck 任务完成或降级的判定方式。
 */
public record AiTaskRecommendationItem(
        String taskType,
        String priority,
        String title,
        String reason,
        String targetRoute,
        String recommendedCommand,
        List<String> evidenceRefs,
        String completionCheck
) {
}
