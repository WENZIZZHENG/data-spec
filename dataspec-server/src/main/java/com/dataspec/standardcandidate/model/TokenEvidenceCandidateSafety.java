package com.dataspec.standardcandidate.model;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 命名证据候选操作的安全摘要。
 *
 * @param readOnly              当前响应是否只读
 * @param writesProject         当前操作是否写入 DataSpec 项目数据
 * @param requiresConfirmation  是否要求显式人工确认
 * @param containsRawSourceText 响应是否包含 raw sourceText
 * @param externalLlmUsed       是否调用外部 LLM
 */
@Schema(description = "命名证据候选操作的稳定安全摘要。")
public record TokenEvidenceCandidateSafety(
        @Schema(description = "当前响应是否只读。", requiredMode = Schema.RequiredMode.REQUIRED) boolean readOnly,
        @Schema(description = "当前操作是否写入 DataSpec 项目数据。", requiredMode = Schema.RequiredMode.REQUIRED) boolean writesProject,
        @Schema(description = "写入前是否要求显式人工确认。", requiredMode = Schema.RequiredMode.REQUIRED) boolean requiresConfirmation,
        @Schema(description = "响应是否包含 raw sourceText；固定为 false。", requiredMode = Schema.RequiredMode.REQUIRED) boolean containsRawSourceText,
        @Schema(description = "是否调用外部 LLM；固定为 false。", requiredMode = Schema.RequiredMode.REQUIRED) boolean externalLlmUsed
) {
}
