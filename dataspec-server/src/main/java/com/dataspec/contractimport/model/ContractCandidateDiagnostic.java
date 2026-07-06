package com.dataspec.contractimport.model;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 契约解析诊断项，描述降级、复杂 schema、截断或输入问题。
 *
 * @param code 稳定诊断码，供测试、CLI 和 AI fixture 断言。
 * @param severity 诊断级别，例如 INFO、WARN、ERROR。
 * @param message 面向用户和 AI 的脱敏说明。
 * @param sourcePath 诊断对应的契约来源路径。
 */
@Schema(description = "契约解析诊断项，描述降级、复杂 schema、截断或输入问题；内容已脱敏。")
public record ContractCandidateDiagnostic(
        @Schema(description = "稳定诊断码，供测试、CLI 和 AI fixture 断言。")
        String code,
        @Schema(description = "诊断级别，例如 INFO、WARN、ERROR。")
        String severity,
        @Schema(description = "面向用户和 AI 的脱敏说明，不包含 raw secret。")
        String message,
        @Schema(description = "诊断对应的脱敏契约来源路径。")
        String sourcePath
) {
}
