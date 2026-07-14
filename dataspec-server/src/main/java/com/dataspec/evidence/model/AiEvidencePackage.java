package com.dataspec.evidence.model;

import com.dataspec.reviewfinding.model.ReviewFinding;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * AI evidence package 的稳定机器契约。
 *
 * @param kind 契约类型标识。
 * @param schemaVersion 契约版本；breaking 变更必须升级。
 * @param packageId 本次生成的证据包 ID。
 * @param projectId 项目 ID。
 * @param generatedAt 证据包生成时间。
 * @param source 证据来源摘要。
 * @param standardSnapshot 标准快照摘要。
 * @param inputsSummary 输入摘要；不得包含 raw secret。
 * @param outputsSummary 输出摘要；不得包含 raw AI output 或 secret。
 * @param validationSummary 原有来源校验摘要。
 * @param postCheckSummary AI 输出后置校验摘要；只保存状态、issue 计数、引用摘要和建议命令。
 * @param findings 从持久化来源派生或经 post-check/evidence 复验的共享 findings。
 * @param artifacts 证据 artifact 摘要。
 * @param nextActions 建议下一步动作。
 * @param suggestedCommands 建议命令。
 * @param diagnostics 诊断信息。
 */
@Schema(description = "AI Evidence Package 的稳定机器契约；所有摘要和 findings 均 bounded、secret-safe。")
public record AiEvidencePackage(
        @Schema(description = "契约类型标识，固定为 dataspec-ai-evidence-package。")
        String kind,
        @Schema(description = "Evidence Package schema 版本；breaking 变更必须升级。")
        Integer schemaVersion,
        @Schema(description = "本次本地生成的唯一 evidence package ID。")
        String packageId,
        @Schema(description = "包所属项目 ID；持久化来源以来源记录的项目为准。")
        Long projectId,
        @Schema(description = "证据包生成时间，使用 UTC Instant。")
        Instant generatedAt,
        @Schema(description = "持久化或 payload-only 证据来源摘要。")
        AiEvidenceSource source,
        @Schema(description = "生成时使用的标准快照摘要；未知时标记为未版本化。")
        AiEvidenceStandardSnapshot standardSnapshot,
        @Schema(description = "有界脱敏输入摘要；不得包含 raw secret。")
        Map<String, Object> inputsSummary,
        @Schema(description = "有界脱敏输出摘要；不得包含 raw AI output。")
        Map<String, Object> outputsSummary,
        @Schema(description = "来源自身的确定性校验摘要。")
        Map<String, Object> validationSummary,
        @Schema(description = "AI output post-check 摘要；只保留 allowlist 字段，不保存 raw content 或 issues。")
        Map<String, Object> postCheckSummary,
        @ArraySchema(
                arraySchema = @Schema(description = "从持久化来源派生或经 post-check/evidence 复验的共享 findings。"),
                schema = @Schema(implementation = ReviewFinding.class))
        List<ReviewFinding> findings,
        @ArraySchema(arraySchema = @Schema(description = "证据 artifact 摘要列表。"), schema = @Schema(implementation = AiEvidenceArtifact.class))
        List<AiEvidenceArtifact> artifacts,
        @ArraySchema(arraySchema = @Schema(description = "建议的人工下一步动作。"), schema = @Schema(type = "string"))
        List<String> nextActions,
        @ArraySchema(arraySchema = @Schema(description = "建议执行的只读或显式命令。"), schema = @Schema(type = "string"))
        List<String> suggestedCommands,
        @ArraySchema(arraySchema = @Schema(description = "证据来源、payload 或兼容性诊断。"), schema = @Schema(implementation = AiEvidenceDiagnostic.class))
        List<AiEvidenceDiagnostic> diagnostics
) {
    public AiEvidencePackage {
        findings = findings == null ? List.of() : List.copyOf(findings);
        artifacts = artifacts == null ? List.of() : List.copyOf(artifacts);
        nextActions = nextActions == null ? List.of() : List.copyOf(nextActions);
        suggestedCommands = suggestedCommands == null ? List.of() : List.copyOf(suggestedCommands);
        diagnostics = diagnostics == null ? List.of() : List.copyOf(diagnostics);
    }
}
