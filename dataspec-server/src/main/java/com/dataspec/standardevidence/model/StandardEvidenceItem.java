package com.dataspec.standardevidence.model;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 跨来源证据视图中的单条安全证据。
 *
 * @param evidenceType 证据类型，如 FIELD_SOURCE、PROVENANCE_CONFIDENCE、USAGE_HEATMAP、CANDIDATE_DECISION。
 * @param evidenceId 证据引用 ID 或稳定摘要 key，不包含 raw payload。
 * @param title 证据标题，供用户快速扫描。
 * @param summary 证据摘要，只包含脱敏后的来源、状态、计数、时间或短原因。
 * @param sourceType 来源类别或作业类型，可为空，输出前必须脱敏。
 * @param status 证据状态或分档，可为空，输出前必须脱敏。
 * @param confidence 候选或来源置信度分数，可为空。
 * @param occurredAt 证据发生或最近命中时间，可为空。
 * @param refs 可追溯的脱敏引用列表，不包含 SQL 原文、AI payload、raw evidence 或连接凭据。
 */
public record StandardEvidenceItem(
        String evidenceType,
        String evidenceId,
        String title,
        String summary,
        String sourceType,
        String status,
        Integer confidence,
        LocalDateTime occurredAt,
        List<String> refs
) {
}
