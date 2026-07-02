package com.dataspec.standardhealth.controller;

import com.dataspec.common.result.R;
import com.dataspec.standardhealth.model.StandardHealthPlan;
import com.dataspec.standardhealth.model.StandardHealthSnapshotCreateReq;
import com.dataspec.standardhealth.model.StandardHealthSnapshotView;
import com.dataspec.standardhealth.model.StandardHealthTrend;
import com.dataspec.standardhealth.service.StandardHealthService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 项目级标准健康快照与趋势。
 */
@RestController
@RequestMapping("/api/standard-health")
@RequiredArgsConstructor
public class StandardHealthController {

    private final StandardHealthService standardHealthService;

    @PostMapping("/snapshots")
    public R<StandardHealthSnapshotView> createSnapshot(@RequestBody StandardHealthSnapshotCreateReq req) {
        return R.ok(standardHealthService.createSnapshot(req));
    }

    @GetMapping("/trend")
    public R<StandardHealthTrend> trend(@RequestParam Long projectId,
                                        @RequestParam(required = false, defaultValue = "30") Integer limit) {
        return R.ok(standardHealthService.trend(projectId, limit));
    }

    @GetMapping("/plan")
    public R<StandardHealthPlan> plan(@RequestParam Long projectId) {
        return R.ok(standardHealthService.plan(projectId));
    }
}
