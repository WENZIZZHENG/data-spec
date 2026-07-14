package com.dataspec.standardcandidate.model;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 命名证据 preview 中可供人工核对的候选写入 payload。
 *
 * @param projectId     候选所属项目 ID
 * @param candidateName 规范化 snake_case 候选字段名
 * @param displayName   可选显示名
 * @param dataType      数据类型草案
 * @param comment       可选且已脱敏的候选说明
 * @param sourceType    固定 TOKEN_EVIDENCE
 * @param sourceRef     已脱敏的稳定来源引用
 * @param evidenceJson  不含 raw sourceText 的结构化证据 JSON
 * @param confidence    低置信人工复核分，范围 0 到 100
 */
@Schema(description = "命名证据候选的可审阅写入 payload；所有文本均已校验、限长并按需脱敏。")
public record TokenEvidenceCandidatePayload(
        @Schema(description = "候选所属项目 ID。", format = "int64", requiredMode = Schema.RequiredMode.REQUIRED)
        Long projectId,
        @Schema(description = "规范化 snake_case 候选字段名。", requiredMode = Schema.RequiredMode.REQUIRED)
        String candidateName,
        @Schema(description = "候选显示名；没有时为空。", nullable = true)
        String displayName,
        @Schema(description = "候选数据类型草案。", requiredMode = Schema.RequiredMode.REQUIRED)
        String dataType,
        @Schema(description = "已脱敏的候选说明；没有时为空。", nullable = true)
        String comment,
        @Schema(description = "候选来源类型，固定为 TOKEN_EVIDENCE。", requiredMode = Schema.RequiredMode.REQUIRED)
        String sourceType,
        @Schema(description = "经脱敏和限长的稳定来源引用。", requiredMode = Schema.RequiredMode.REQUIRED)
        String sourceRef,
        @Schema(description = "不含 raw sourceText 的结构化命名证据 JSON。", requiredMode = Schema.RequiredMode.REQUIRED)
        String evidenceJson,
        @Schema(description = "低置信人工复核分，范围 0 到 100。", minimum = "0", maximum = "100", requiredMode = Schema.RequiredMode.REQUIRED)
        int confidence
) {
}
