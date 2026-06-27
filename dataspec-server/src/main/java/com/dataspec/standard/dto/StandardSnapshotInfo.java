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
        boolean versioned
) {

    public static StandardSnapshotInfo unversioned(Long projectId) {
        return new StandardSnapshotInfo(null, projectId, "unversioned", null, null, null, null, false);
    }
}
