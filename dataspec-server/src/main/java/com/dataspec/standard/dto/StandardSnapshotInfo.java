package com.dataspec.standard.dto;

import java.time.LocalDateTime;

/**
 * 标准快照元数据。
 */
public record StandardSnapshotInfo(
        Long snapshotId,
        Long projectId,
        String specVersion,
        String name,
        String description,
        String specHash,
        LocalDateTime createdAt,
        boolean versioned,
        String source
) {

    public StandardSnapshotInfo(
            Long snapshotId,
            Long projectId,
            String specVersion,
            String name,
            String description,
            String specHash,
            LocalDateTime createdAt,
            boolean versioned
    ) {
        this(snapshotId, projectId, specVersion, name, description, specHash, createdAt, versioned,
                versioned ? "current" : "unversioned");
    }

    public static StandardSnapshotInfo unversioned(Long projectId) {
        return new StandardSnapshotInfo(null, projectId, "unversioned", null, null, null, null, false, "unversioned");
    }

    public StandardSnapshotInfo withSource(String source) {
        return new StandardSnapshotInfo(snapshotId, projectId, specVersion, name, description, specHash, createdAt, versioned, source);
    }
}
