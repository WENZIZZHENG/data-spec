package com.dataspec.bootstrap.model;

public record AiSessionBootstrapSnapshot(
        Long snapshotId,
        Long projectId,
        String specVersion,
        String specHash,
        boolean versioned,
        String source
) {
}
