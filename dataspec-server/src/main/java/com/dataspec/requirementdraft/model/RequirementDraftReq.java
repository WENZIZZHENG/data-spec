package com.dataspec.requirementdraft.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 自然语言需求草案请求。
 *
 * <p>第一版只做只读分析，不会自动写入标准字段或候选 Inbox。</p>
 */
public record RequirementDraftReq(
        @NotNull Long projectId,
        @NotBlank String description,
        @NotBlank String targetTableName,
        String groupHint,
        Integer limit
) {
}
