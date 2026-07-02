package com.dataspec.standardhealth.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 标准健康趋势响应。
 */
@Data
public class StandardHealthTrend {

    private Long projectId;

    private StandardHealthSnapshotView latest;

    private List<StandardHealthSnapshotView> snapshots = new ArrayList<>();

    private StandardHealthDelta weekDelta;

    private StandardHealthDelta monthDelta;

    private List<String> nextActions = new ArrayList<>();
}
