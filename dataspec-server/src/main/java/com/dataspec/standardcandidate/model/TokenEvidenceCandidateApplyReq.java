package com.dataspec.standardcandidate.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 命名证据候选确认写入请求。
 *
 * @param previewInput 与 preview 完全一致的输入；服务端会重新计算 evidence
 * @param dryRunToken  preview 签发的进程内 HMAC token
 * @param confirmed    用户是否显式确认本次写入
 */
@Schema(description = "命名证据候选确认写入请求；输入或 glossary 漂移后必须重新 preview。")
public record TokenEvidenceCandidateApplyReq(
        @NotNull
        @Valid
        @Schema(description = "与 preview 完全一致的候选输入。", requiredMode = Schema.RequiredMode.REQUIRED)
        TokenEvidenceCandidatePreviewReq previewInput,
        @NotBlank
        @Schema(description = "preview 返回的签名 dry-run token；服务重启或 evidence 漂移后失效。", requiredMode = Schema.RequiredMode.REQUIRED)
        String dryRunToken,
        @NotNull
        @Schema(description = "必须为 true，表示用户已核对候选、来源和 token evidence。", requiredMode = Schema.RequiredMode.REQUIRED)
        Boolean confirmed
) {
}
