package com.dataspec.capability.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 面向 AI/CLI/MCP 的稳定能力条目。
 *
 * <p>旧字段如 {@code writeRisk}/{@code preflightChecks}/{@code nextActions} 保持兼容；新增 {@code safety}
 * 提供可机器判断的写入安全协议，调用方不得把该目录当作授权结果或执行结果。</p>
 */
public record AiCapabilityEntry(
        String id,
        String category,
        String title,
        String summary,
        String status,
        String stability,
        boolean requiresProject,
        String writeRisk,
        List<String> requiredInputs,
        List<String> optionalInputs,
        List<String> outputContracts,
        List<String> apiEndpoints,
        List<String> cliCommands,
        List<String> mcpResources,
        List<String> mcpTools,
        List<String> frontendRoutes,
        List<String> contractIds,
        List<String> workflowIds,
        List<String> profileIds,
        List<AiCapabilityExample> examples,
        List<String> preflightChecks,
        List<String> nextActions,
        @Schema(description = "AI 写入安全元数据；新增兼容字段，描述是否只读、是否写项目、dry-run/幂等要求、敏感输入名和安全下一步。")
        AiWriteSafetyMetadata safety,
        String docsRef
) {
}
