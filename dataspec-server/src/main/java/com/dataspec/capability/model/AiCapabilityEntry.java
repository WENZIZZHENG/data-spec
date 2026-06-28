package com.dataspec.capability.model;

import java.util.List;

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
        String docsRef
) {
}
