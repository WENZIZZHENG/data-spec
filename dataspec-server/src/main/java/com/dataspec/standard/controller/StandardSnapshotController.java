package com.dataspec.standard.controller;

import com.dataspec.common.result.R;
import com.dataspec.standard.dto.StandardSnapshotCreateReq;
import com.dataspec.standard.dto.StandardSnapshotInfo;
import com.dataspec.standard.service.StandardSnapshotService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 标准版本快照 API。
 */
@RestController
@RequestMapping("/api/projects/{projectId}/standard-snapshots")
@RequiredArgsConstructor
public class StandardSnapshotController {

    private final StandardSnapshotService standardSnapshotService;

    @PostMapping
    public R<StandardSnapshotInfo> createSnapshot(
            @PathVariable Long projectId,
            @RequestBody StandardSnapshotCreateReq req,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey
    ) {
        return R.ok(standardSnapshotService.createSnapshot(projectId, req, idempotencyKey));
    }

    @GetMapping("/current")
    public R<StandardSnapshotInfo> currentSnapshot(@PathVariable Long projectId) {
        return R.ok(standardSnapshotService.getCurrentSnapshot(projectId));
    }

    @GetMapping
    public R<List<StandardSnapshotInfo>> listSnapshots(@PathVariable Long projectId) {
        return R.ok(standardSnapshotService.listSnapshots(projectId));
    }
}
