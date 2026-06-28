package com.dataspec.evidence.model;

public record AiEvidenceSource(
        EvidenceSourceType sourceType,
        Long sourceId,
        String sourceTitle,
        String status,
        boolean persisted
) {
}
