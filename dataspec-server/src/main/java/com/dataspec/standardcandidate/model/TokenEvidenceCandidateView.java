package com.dataspec.standardcandidate.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * 命名证据 apply 返回的候选安全视图。
 *
 * <p>该 DTO 不暴露持久化逻辑删除字段，避免 Entity 后续扩展静默改变公共 API。</p>
 *
 * @param id             候选 ID
 * @param projectId      候选所属项目 ID
 * @param candidateName  候选字段名
 * @param displayName    可选显示名
 * @param dataType       数据类型草案
 * @param comment        可选且已脱敏的说明
 * @param sourceType     固定 TOKEN_EVIDENCE
 * @param sourceRef      已脱敏的稳定来源引用
 * @param evidenceJson   不含 raw sourceText 的结构化证据 JSON
 * @param confidence     低置信人工复核分
 * @param status         候选决策状态，apply 后为 PENDING
 * @param targetFieldId  后续合并或采纳产生的目标字段 ID
 * @param decisionReason 后续人工决策原因
 * @param decidedAt      后续人工决策时间
 * @param createdAt      候选创建时间
 * @param updatedAt      候选更新时间
 */
@Schema(description = "TOKEN_EVIDENCE 候选安全视图；不包含逻辑删除等持久化内部字段。")
public record TokenEvidenceCandidateView(
        @Schema(description = "候选 ID。", format = "int64", requiredMode = Schema.RequiredMode.REQUIRED)
        Long id,
        @Schema(description = "候选所属项目 ID。", format = "int64", requiredMode = Schema.RequiredMode.REQUIRED)
        Long projectId,
        @Schema(description = "候选字段名。", requiredMode = Schema.RequiredMode.REQUIRED)
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
        Integer confidence,
        @Schema(description = "候选决策状态；首次 apply 后为 PENDING。", requiredMode = Schema.RequiredMode.REQUIRED)
        String status,
        @Schema(description = "后续采纳或合并产生的目标字段 ID；PENDING 时为空。", nullable = true, format = "int64")
        Long targetFieldId,
        @Schema(description = "后续人工决策原因；PENDING 时为空。", nullable = true)
        String decisionReason,
        @Schema(description = "后续人工决策时间；PENDING 时为空。", nullable = true, format = "date-time")
        LocalDateTime decidedAt,
        @Schema(description = "候选创建时间。", format = "date-time", requiredMode = Schema.RequiredMode.REQUIRED)
        LocalDateTime createdAt,
        @Schema(description = "候选最近更新时间。", format = "date-time", requiredMode = Schema.RequiredMode.REQUIRED)
        LocalDateTime updatedAt
) {
}
