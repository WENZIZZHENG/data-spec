package com.dataspec.contractimport.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 契约候选导入预览包，是 API、CLI、AI fixture 共享的稳定 JSON 契约。
 *
 * @param kind 契约类型，固定为 dataspec.contract-candidate-preview。
 * @param schemaVersion 输出 schema 版本。
 * @param projectId DataSpec 项目 ID。
 * @param sourceKind 契约来源类型。
 * @param sourcePath 脱敏后的契约来源路径或人读标识。
 * @param contractHash 基于脱敏契约内容、解析参数和候选摘要生成的稳定 SHA-256。
 * @param summary 候选数量、重复、匹配和诊断摘要。
 * @param candidateFields 可审核的字段候选集合，不会自动写入候选库。
 * @param diagnostics 解析降级、复杂 schema 和截断等脱敏诊断。
 * @param safety 只读、无外部网络和无真实业务行数据的安全声明。
 * @param nextActions 后续人工审核和候选入箱建议。
 */
@Schema(description = "契约候选导入预览包；API、CLI 和 AI fixture 共享的只读稳定 JSON 契约。")
public record ContractCandidatePreviewPackage(
        @Schema(description = "契约类型，固定为 dataspec.contract-candidate-preview。")
        String kind,
        @Schema(description = "输出 schema 版本，兼容性新增字段时递增。")
        int schemaVersion,
        @Schema(description = "DataSpec 项目 ID。")
        Long projectId,
        @Schema(description = "契约来源类型：openapi、json-schema 或 protobuf。")
        String sourceKind,
        @Schema(description = "脱敏后的契约来源路径或人读标识。")
        String sourcePath,
        @Schema(description = "基于完整脱敏契约内容、来源路径、解析参数和候选摘要生成的稳定 SHA-256。")
        String contractHash,
        @Schema(description = "候选数量、重复、匹配和诊断摘要。")
        ContractCandidateSummary summary,
        @Schema(description = "可审核的字段候选集合；预览不会自动写入候选库或正式字段。")
        List<ContractCandidateField> candidateFields,
        @Schema(description = "解析降级、复杂 schema 和截断等脱敏诊断。")
        List<ContractCandidateDiagnostic> diagnostics,
        @Schema(description = "只读、无外部网络、无外部 LLM 和无真实业务行数据的安全声明。")
        ContractCandidateSafety safety,
        @Schema(description = "后续人工审核和候选入箱建议；不会包含 raw secret。")
        List<String> nextActions
) {
}
