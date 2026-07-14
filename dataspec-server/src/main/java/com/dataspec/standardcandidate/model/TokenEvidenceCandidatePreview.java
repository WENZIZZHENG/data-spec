package com.dataspec.standardcandidate.model;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 命名证据候选 dry-run 预览。
 *
 * @param kind                 稳定响应类型
 * @param schemaVersion        响应 schema 版本
 * @param projectId            候选所属项目 ID
 * @param candidateName        规范化候选字段名
 * @param sourceType           固定 TOKEN_EVIDENCE
 * @param sourceRef            经脱敏和限长的稳定来源引用
 * @param status               预览状态
 * @param willWrite            preview 固定为 false
 * @param duplicateCandidateId exact duplicate 或 name conflict 的既有候选 ID
 * @param inboxPayload         可审阅的命名证据候选 payload
 * @param signals              有界命名证据信号
 * @param dryRunToken          READY 时签发的确认 token
 * @param safety               稳定安全摘要
 * @param nextActions          可恢复下一步
 */
@Schema(description = "命名证据候选 dry-run 预览；该响应本身不会写入 Inbox。")
public record TokenEvidenceCandidatePreview(
        @Schema(description = "稳定类型，固定为 dataspec.token-evidence-candidate-preview。", requiredMode = Schema.RequiredMode.REQUIRED) String kind,
        @Schema(description = "响应 schema 版本，当前为 1。", requiredMode = Schema.RequiredMode.REQUIRED) int schemaVersion,
        @Schema(description = "候选所属项目 ID。", format = "int64", requiredMode = Schema.RequiredMode.REQUIRED) Long projectId,
        @Schema(description = "规范化候选字段名。", requiredMode = Schema.RequiredMode.REQUIRED) String candidateName,
        @Schema(description = "候选来源类型，固定为 TOKEN_EVIDENCE。", requiredMode = Schema.RequiredMode.REQUIRED) String sourceType,
        @Schema(description = "经脱敏和限长的稳定来源引用。", requiredMode = Schema.RequiredMode.REQUIRED) String sourceRef,
        @Schema(description = "预览状态。", requiredMode = Schema.RequiredMode.REQUIRED) TokenEvidenceCandidatePreviewStatus status,
        @Schema(description = "preview 固定为 false。", requiredMode = Schema.RequiredMode.REQUIRED) boolean willWrite,
        @Schema(description = "冲突或重复的既有候选 ID；没有时为空。", nullable = true, format = "int64") Long duplicateCandidateId,
        @Schema(description = "可审阅且不含 raw sourceText 的命名证据候选 payload。", requiredMode = Schema.RequiredMode.REQUIRED)
        TokenEvidenceCandidatePayload inboxPayload,
        @ArraySchema(
                arraySchema = @Schema(description = "触发本候选的有界命名证据信号。", requiredMode = Schema.RequiredMode.REQUIRED),
                schema = @Schema(description = "单条命名证据信号。"))
        List<TokenEvidenceCandidateSignal> signals,
        @Schema(description = "READY 时签发的进程内 dry-run token；其他状态为空。", nullable = true) String dryRunToken,
        @Schema(description = "稳定安全摘要。", requiredMode = Schema.RequiredMode.REQUIRED) TokenEvidenceCandidateSafety safety,
        @ArraySchema(
                arraySchema = @Schema(description = "当前状态对应的可恢复下一步。", requiredMode = Schema.RequiredMode.REQUIRED),
                schema = @Schema(description = "单条下一步建议。"))
        List<String> nextActions
) {
}
