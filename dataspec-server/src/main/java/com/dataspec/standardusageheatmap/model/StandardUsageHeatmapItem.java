package com.dataspec.standardusageheatmap.model;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 单个标准字段的使用热区与治理优先级。
 *
 * @param fieldId 标准字段 ID。
 * @param name 字段英文名或技术名。
 * @param displayName 字段业务显示名，可为空。
 * @param status 字段生命周期状态，如 enabled、draft、deprecated、disabled。
 * @param sourceKinds 脱敏后的来源类型集合，不包含 raw source metadata。
 * @param qualityScore 字段质量评分，缺少质量报告时为空。
 * @param qualityLevel 字段质量分档名称，缺少质量报告时为空。
 * @param conflictCount 字段参与的冲突组数量。
 * @param sourceEvidenceCount 字段来源记录数量。
 * @param lintHits 近期 SQL 检查记录中命中字段名的次数。
 * @param aiJobHits 近期 AI 作业摘要中命中字段名的次数。
 * @param lastReferencedAt 最近一次 SQL 检查或 AI 作业命中时间，未命中时为空。
 * @param usageScore 0 到 100 的近期使用热度分。
 * @param cleanupPriority 0 到 100 的治理或清理优先级。
 * @param suggestedNextAction 建议用户或 AI 下一步如何处理该字段。
 */
public record StandardUsageHeatmapItem(
        Long fieldId,
        String name,
        String displayName,
        String status,
        List<String> sourceKinds,
        Integer qualityScore,
        String qualityLevel,
        int conflictCount,
        int sourceEvidenceCount,
        int lintHits,
        int aiJobHits,
        LocalDateTime lastReferencedAt,
        int usageScore,
        int cleanupPriority,
        String suggestedNextAction
) {
}
