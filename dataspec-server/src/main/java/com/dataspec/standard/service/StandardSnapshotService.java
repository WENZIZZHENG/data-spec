package com.dataspec.standard.service;

import com.dataspec.standard.dto.StandardSnapshotCreateReq;
import com.dataspec.standard.dto.StandardSnapshotInfo;

import java.util.List;

/**
 * 标准版本快照服务。
 */
public interface StandardSnapshotService {

    StandardSnapshotInfo createSnapshot(Long projectId, StandardSnapshotCreateReq req);

    StandardSnapshotInfo getCurrentSnapshot(Long projectId);

    List<StandardSnapshotInfo> listSnapshots(Long projectId);
}
