package com.dataspec.projectbackup.model;

import java.time.LocalDateTime;
import java.util.List;

public record ProjectBackupPackage(
        Integer schemaVersion,
        LocalDateTime exportedAt,
        BackupProject sourceProject,
        ProjectBackupAssets assets,
        ProjectBackupCounts counts,
        ProjectBackupSanitization sanitization,
        List<String> warnings,
        String packageHash
) {
}
