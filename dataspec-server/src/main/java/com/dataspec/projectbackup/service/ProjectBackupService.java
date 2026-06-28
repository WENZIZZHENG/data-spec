package com.dataspec.projectbackup.service;

import com.dataspec.projectbackup.entity.ProjectRestoreRecord;
import com.dataspec.projectbackup.model.ProjectBackupPackage;
import com.dataspec.projectbackup.model.ProjectRestorePlan;
import com.dataspec.projectbackup.model.ProjectRestoreReq;
import com.dataspec.projectbackup.model.ProjectRestoreResult;

import java.util.List;

public interface ProjectBackupService {

    ProjectBackupPackage exportPackage(Long projectId);

    ProjectRestorePlan previewRestore(ProjectRestoreReq req);

    ProjectRestoreResult applyRestore(ProjectRestoreReq req);

    List<ProjectRestoreRecord> listRestoreRecords(Long projectId);
}
