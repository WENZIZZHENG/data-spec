package com.dataspec.evidence.model;

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
 * @param artifacts 证据 artifact 摘要。
 * @param nextActions 建议下一步动作。
 * @param suggestedCommands 建议命令。
 * @param diagnostics 诊断信息。
 */
public record AiEvidencePackage(
        String kind,
        Integer schemaVersion,
        String packageId,
        Long projectId,
        Instant generatedAt,
        AiEvidenceSource source,
        AiEvidenceStandardSnapshot standardSnapshot,
        Map<String, Object> inputsSummary,
        Map<String, Object> outputsSummary,
        Map<String, Object> validationSummary,
        Map<String, Object> postCheckSummary,
        List<AiEvidenceArtifact> artifacts,
        List<String> nextActions,
        List<String> suggestedCommands,
        List<AiEvidenceDiagnostic> diagnostics
) {
}
