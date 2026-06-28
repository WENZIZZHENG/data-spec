package com.dataspec.projectbackup.model;

import com.dataspec.projectbackup.entity.ProjectRestoreRecord;

public record ProjectRestoreResult(
        ProjectRestorePlan plan,
        ProjectRestoreRecord record
) {
}
