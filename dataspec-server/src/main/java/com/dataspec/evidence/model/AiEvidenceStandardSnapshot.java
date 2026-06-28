package com.dataspec.evidence.model;

public record AiEvidenceStandardSnapshot(
        Long snapshotId,
        String specVersion,
        String specHash,
        boolean versioned
) {
    public static AiEvidenceStandardSnapshot unversioned() {
        return new AiEvidenceStandardSnapshot(null, "unversioned", null, false);
    }
}
