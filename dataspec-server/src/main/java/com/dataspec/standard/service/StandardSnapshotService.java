package com.dataspec.standard.service;

import com.dataspec.standard.dto.StandardSnapshotCreateReq;
import com.dataspec.standard.dto.StandardSnapshotInfo;
import com.dataspec.standard.dto.StandardSnapshotPayload;

import java.util.List;

/**
 * 标准版本快照服务。
 */
public interface StandardSnapshotService {

    StandardSnapshotInfo createSnapshot(Long projectId, StandardSnapshotCreateReq req);

    StandardSnapshotInfo createSnapshot(Long projectId, StandardSnapshotCreateReq req, String idempotencyKey);

    StandardSnapshotInfo getCurrentSnapshot(Long projectId);

    List<StandardSnapshotInfo> listSnapshots(Long projectId);

    StandardSnapshotPayload getSnapshotPayload(Long projectId, Long snapshotId);

    StandardSnapshotPayload getSnapshotPayloadByVersion(Long projectId, String version);
}
