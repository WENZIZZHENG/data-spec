package com.dataspec.bootstrap.model;

import java.util.List;

public record AiSessionBootstrapCapability(
        String id,
        String title,
        String status,
        String writeRisk,
        boolean requiresProject,
        List<String> apiEndpoints,
        List<String> cliCommands,
        List<String> mcpResources,
        List<String> mcpTools,
        List<String> nextActions
) {
}
