package com.dataspec.evidence.model;

import java.util.Map;

public record AiEvidenceArtifact(
        String artifactType,
        String title,
        String format,
        Map<String, Object> summary
) {
}
