package com.dataspec.evidence.model;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Evidence Package 的来源摘要。
 *
 * @param sourceType  来源类型
 * @param sourceId    持久化来源 ID；payload-only 来源为空
 * @param sourceTitle 脱敏后的来源标题
 * @param status      来源状态
 * @param persisted   来源是否持久化并可重新读取
 * @param evidenceRef 持久化来源的 canonical evidence ref；payload-only 来源为空
 */
public record AiEvidenceSource(
        @Schema(description = "Evidence Package 来源类型；决定来源 ID 和 evidenceRef 的解析语义。")
        EvidenceSourceType sourceType,
        @Schema(description = "持久化来源记录 ID；payload-only 来源为空。")
        Long sourceId,
        @Schema(description = "脱敏后的来源标题。")
        String sourceTitle,
        @Schema(description = "来源记录或 payload 的当前状态。")
        String status,
        @Schema(description = "来源是否已持久化并可由 sourceId 重新读取。")
        boolean persisted,
        @Schema(description = "项目级 canonical evidence ref，格式为 dataspec://evidence/<source-type>/<source-id>；非持久化来源为空。")
        String evidenceRef
) {
}
