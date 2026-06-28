package com.dataspec.capability.model;

import java.time.LocalDateTime;
import java.util.List;

public record AiCapabilityCatalog(
        String kind,
        int schemaVersion,
        String catalogVersion,
        LocalDateTime generatedAt,
        Long projectId,
        List<AiCapabilityEntry> capabilities,
        List<String> requiredCapabilityIds,
        List<String> recommendedFirstActions,
        List<AiCapabilityDiagnostic> diagnostics
) {
}
