package com.dataspec.standardcandidate.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 命名证据候选预览输入。
 *
 * @param projectId     候选所属项目 ID
 * @param candidateName 待进入 Inbox 的 snake_case 候选字段名
 * @param displayName   可选中文或业务显示名
 * @param dataType      候选字段的数据类型草案
 * @param comment       可选候选说明；入库前会脱敏和限长
 * @param sourceRef     调用方提供的稳定来源引用，用于事实去重
 * @param sourceText    可选命名解析文本；为空时使用候选名和显示名，不会原文入库
 */
@Schema(description = "命名证据候选预览输入；preview 只读，apply 时必须原样带回并重新校验证据。")
public record TokenEvidenceCandidatePreviewReq(
        @NotNull
        @Schema(description = "候选所属项目 ID；调用方必须拥有项目访问权限。", requiredMode = Schema.RequiredMode.REQUIRED)
        Long projectId,
        @NotBlank
        @Size(max = 100)
        @Pattern(regexp = "^[a-z][a-z0-9_]*$", message = "候选字段名必须是 snake_case")
        @Schema(description = "待进入 Inbox 的 snake_case 候选字段名，最长 100 个字符。", requiredMode = Schema.RequiredMode.REQUIRED)
        String candidateName,
        @Size(max = 100)
        @Schema(description = "候选显示名，最长 100 个字符；可为空。", nullable = true)
        String displayName,
        @NotBlank
        @Size(max = 50)
        @Schema(description = "候选数据类型草案，最长 50 个字符。", requiredMode = Schema.RequiredMode.REQUIRED)
        String dataType,
        @Size(max = 1000)
        @Schema(description = "候选说明，最长 1000 个字符；保存前会脱敏。", nullable = true)
        String comment,
        @NotBlank
        @Size(max = 300)
        @Schema(description = "稳定来源引用，例如 field:orders.ord_amt；最长 300 个字符，保存前会脱敏。", requiredMode = Schema.RequiredMode.REQUIRED)
        String sourceRef,
        @Size(max = 512)
        @Schema(description = "用于命名解析的可选来源文本；为空时使用候选名和显示名，原文不会保存或返回。", nullable = true)
        String sourceText
) {
}
