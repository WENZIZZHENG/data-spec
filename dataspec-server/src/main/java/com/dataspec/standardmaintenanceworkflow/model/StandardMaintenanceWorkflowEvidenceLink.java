package com.dataspec.standardmaintenanceworkflow.model;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 标准维护 workflow 证据链接或安全摘要。
 *
 * @param sourceCapability 来源能力，例如 standard-candidate-inbox 或 field-quality-scoring。
 * @param label 人类可读证据名称。
 * @param targetRoute 可打开的 DataSpec 页面或 API 模板，不包含凭据。
 * @param summary 脱敏证据摘要。
 * @param count 该证据代表的待处理项数量。
 */
@Schema(description = "标准维护 workflow 证据链接或安全摘要。")
public record StandardMaintenanceWorkflowEvidenceLink(
        @Schema(description = "来源能力，例如 standard-candidate-inbox 或 field-quality-scoring。")
        String sourceCapability,
        @Schema(description = "人类可读证据名称。")
        String label,
        @Schema(description = "可打开的 DataSpec 页面或 API 模板，不包含凭据。")
        String targetRoute,
        @Schema(description = "脱敏证据摘要。")
        String summary,
        @Schema(description = "该证据代表的待处理项数量。")
        int count
) {
}
