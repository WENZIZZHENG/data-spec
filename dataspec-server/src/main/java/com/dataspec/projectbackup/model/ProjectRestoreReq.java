package com.dataspec.projectbackup.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record ProjectRestoreReq(
        Long targetProjectId,
        Boolean overwrite,
        @Valid @NotNull(message = "备份包不能为空") ProjectBackupPackage backupPackage
) {
}
