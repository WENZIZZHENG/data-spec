package com.dataspec.standardhealth.service;

import com.dataspec.standardhealth.model.StandardHealthPlan;
import com.dataspec.standardhealth.model.StandardHealthSnapshotCreateReq;
import com.dataspec.standardhealth.model.StandardHealthSnapshotView;
import com.dataspec.standardhealth.model.StandardHealthTrend;

public interface StandardHealthService {

    StandardHealthSnapshotView createSnapshot(StandardHealthSnapshotCreateReq req);

    StandardHealthTrend trend(Long projectId, Integer limit);

    StandardHealthPlan plan(Long projectId);
}
