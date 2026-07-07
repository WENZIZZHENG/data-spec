package com.dataspec.standardevidence.model;

import java.util.List;

/**
 * 标准对象跨来源证据视图响应。
 *
 * @param projectId 本次查询所属项目 ID。
 * @param subject 目标标准对象摘要。
 * @param summary 跨来源证据聚合摘要。
 * @param items 安全证据列表，默认按证据时间倒序和证据类型排序。
 * @param aiEvidenceSummary 可复制给 AI 的脱敏摘要，只复述 items 和 summary 中的安全事实。
 * @param coverageNotes 覆盖说明，列出缺少或近似匹配的证据类别。
 */
public record StandardEvidenceReport(
        Long projectId,
        StandardEvidenceSubject subject,
        StandardEvidenceSummary summary,
        List<StandardEvidenceItem> items,
        String aiEvidenceSummary,
        List<String> coverageNotes
) {
}
