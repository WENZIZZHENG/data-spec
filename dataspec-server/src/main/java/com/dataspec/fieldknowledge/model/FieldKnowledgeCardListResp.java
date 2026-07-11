package com.dataspec.fieldknowledge.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 字段知识卡列表响应，包含裁剪信息，避免 AI Context 或前端一次性读取过多字段。
 *
 * @param projectId 所属项目 ID
 * @param totalMatched 命中字段总数
 * @param returnedCount 返回卡片数量
 * @param truncated 是否因 limit 被裁剪
 * @param cards 字段知识卡列表
 */
@Schema(description = "字段知识卡列表响应。")
public record FieldKnowledgeCardListResp(
        Long projectId,
        int totalMatched,
        int returnedCount,
        boolean truncated,
        List<FieldKnowledgeCardResp> cards
) {
}
