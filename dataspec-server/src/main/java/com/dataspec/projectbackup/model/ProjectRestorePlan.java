package com.dataspec.projectbackup.model;

import java.util.List;

public record ProjectRestorePlan(
        Boolean dryRun,
        Boolean overwrite,
        Boolean canApply,
        String compatibilityStatus,
        Long targetProjectId,
        String targetProjectName,
        ProjectRestoreCounts counts,
        List<ProjectRestoreItem> items,
        List<String> warnings
) {
}
