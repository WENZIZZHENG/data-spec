package com.dataspec.contractimport.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 契约候选导入预览的安全边界声明。
 *
 * @param readOnly 是否只读。
 * @param writesProject 是否写入项目记录；预览能力固定为 false。
 * @param externalNetworkUsed 是否访问外部网络；第一版固定为 false。
 * @param externalLlmUsed 是否调用外部 LLM；第一版固定为 false。
 * @param containsRealBusinessRows 是否包含真实业务行数据；预览只输出字段摘要，固定为 false。
 * @param sensitiveInputs 输入中检测到并已脱敏的敏感来源类型。
 */
@Schema(description = "契约候选导入预览的安全边界声明；用于 AI/CLI 判断只读和敏感输入边界。")
public record ContractCandidateSafety(
        @Schema(description = "是否只读；契约预览固定为 true。")
        boolean readOnly,
        @Schema(description = "是否写入项目记录；契约预览固定为 false。")
        boolean writesProject,
        @Schema(description = "是否访问外部网络；第一版固定为 false。")
        boolean externalNetworkUsed,
        @Schema(description = "是否调用外部 LLM；第一版固定为 false。")
        boolean externalLlmUsed,
        @Schema(description = "是否包含真实业务行数据；预览只输出字段摘要，固定为 false。")
        boolean containsRealBusinessRows,
        @Schema(description = "输入中检测到并已脱敏的敏感来源类型，例如 sourcePath 或 contractContent。")
        List<String> sensitiveInputs
) {
}
