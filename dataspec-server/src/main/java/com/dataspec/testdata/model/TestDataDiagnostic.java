package com.dataspec.testdata.model;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 测试数据包生成诊断。
 *
 * @param code 稳定诊断编码。
 * @param severity 诊断级别：INFO、WARN 或 ERROR。
 * @param message 已脱敏的人可读说明。
 */
@Schema(description = "测试数据包生成诊断；用于说明 fallback、脱敏、缺口或边界。")
public record TestDataDiagnostic(
        @Schema(description = "稳定诊断编码。")
        String code,
        @Schema(description = "诊断级别：INFO、WARN 或 ERROR。")
        String severity,
        @Schema(description = "已脱敏的人可读说明。")
        String message
) {
}
