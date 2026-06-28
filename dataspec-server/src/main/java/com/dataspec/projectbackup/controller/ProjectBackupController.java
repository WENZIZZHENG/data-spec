package com.dataspec.projectbackup.controller;

import com.dataspec.common.result.R;
import com.dataspec.projectbackup.entity.ProjectRestoreRecord;
import com.dataspec.projectbackup.model.ProjectBackupPackage;
import com.dataspec.projectbackup.model.ProjectRestorePlan;
import com.dataspec.projectbackup.model.ProjectRestoreReq;
import com.dataspec.projectbackup.model.ProjectRestoreResult;
import com.dataspec.projectbackup.service.ProjectBackupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/project-backups")
@RequiredArgsConstructor
public class ProjectBackupController {

    private final ProjectBackupService projectBackupService;

    @GetMapping("/export")
    public R<ProjectBackupPackage> export(@RequestParam Long projectId) {
        return R.ok(projectBackupService.exportPackage(projectId));
    }

    @PostMapping("/restore/preview")
    public R<ProjectRestorePlan> previewRestore(@Valid @RequestBody ProjectRestoreReq req) {
        return R.ok(projectBackupService.previewRestore(req));
    }

    @PostMapping("/restore/apply")
    public R<ProjectRestoreResult> applyRestore(@Valid @RequestBody ProjectRestoreReq req) {
        return R.ok(projectBackupService.applyRestore(req));
    }

    @GetMapping("/restore/records")
    public R<List<ProjectRestoreRecord>> listRestoreRecords(@RequestParam Long projectId) {
        return R.ok(projectBackupService.listRestoreRecords(projectId));
    }
}
