package com.dataspec.bootstrap.model;

import java.time.LocalDateTime;
import java.util.List;

public record AiSessionBootstrap(
        String kind,
        int schemaVersion,
        LocalDateTime generatedAt,
        String status,
        Long projectId,
        String server,
        String authMode,
        String specVersion,
        AiSessionBootstrapSnapshot standardSnapshot,
        List<AiSessionBootstrapCapability> availableCapabilities,
        List<String> recommendedCommands,
        List<String> knownRisks,
        List<String> docsRefs,
        List<AiSessionBootstrapCheck> checks,
        List<AiSessionBootstrapNextAction> nextActions
) {
}
