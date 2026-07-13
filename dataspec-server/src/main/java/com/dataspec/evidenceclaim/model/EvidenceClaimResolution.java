package com.dataspec.evidenceclaim.model;

import com.dataspec.evidence.model.EvidenceSourceType;

/**
 * Evidence claim 的内部解析结果。
 *
 * @param inputRef       经过脱敏和长度限制的输入引用
 * @param canonicalRef   仅 VERIFIED 时返回的 canonical evidence ref
 * @param status         确定性解析状态
 * @param sourceType     由 allowlist URI 解析出的来源类型；格式不支持时为空
 * @param sourceId       输入 URI 中的持久化来源 ID；格式不支持时为空
 * @param sourceProjectId 仅 VERIFIED 时可返回当前项目 ID；跨项目时必须为空
 */
public record EvidenceClaimResolution(
        String inputRef,
        String canonicalRef,
        EvidenceClaimResolutionStatus status,
        EvidenceSourceType sourceType,
        Long sourceId,
        Long sourceProjectId
) {
}
