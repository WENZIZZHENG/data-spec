package com.dataspec.standardevidence.model;

import java.time.LocalDateTime;

/**
 * 单个标准对象的跨来源证据聚合摘要。
 *
 * @param totalEvidenceCount 本次视图返回的安全证据条数。
 * @param confidenceLevel 来源可信度分档，缺少可信度报告时为 UNKNOWN。
 * @param aiConfidence AI 可参考的 0 到 100 置信度分数。
 * @param usageScore 近期使用热度分，缺少热区报告时为 0。
 * @param lintHitCount 近期 SQL 检查摘要中命中该字段名的次数，不代表真实血缘。
 * @param aiJobHitCount 近期 AI 作业摘要中命中该字段名的次数，不包含 raw payload。
 * @param candidateDecisionCount 与该字段相关的候选决策或待处理候选数量。
 * @param changeLogCount 与该字段相关的标准变更日志数量。
 * @param lastEvidenceAt 最近一条安全证据的发生时间，可为空。
 * @param reviewRequired 是否建议人工复核，通常由低置信度、低证据或高治理优先级触发。
 */
public record StandardEvidenceSummary(
        int totalEvidenceCount,
        String confidenceLevel,
        int aiConfidence,
        int usageScore,
        int lintHitCount,
        int aiJobHitCount,
        int candidateDecisionCount,
        int changeLogCount,
        LocalDateTime lastEvidenceAt,
        boolean reviewRequired
) {
}
