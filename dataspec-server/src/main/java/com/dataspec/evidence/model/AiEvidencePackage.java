package com.dataspec.evidence.model;

import java.time.Instant;
import java.util.List;
import java.util.Map;

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
        List<AiEvidenceArtifact> artifacts,
        List<String> nextActions,
        List<String> suggestedCommands,
        List<AiEvidenceDiagnostic> diagnostics
) {
}
