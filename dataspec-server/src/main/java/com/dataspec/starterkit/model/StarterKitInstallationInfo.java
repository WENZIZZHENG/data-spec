package com.dataspec.starterkit.model;

import java.time.LocalDateTime;
import java.util.List;

public record StarterKitInstallationInfo(
        Long id,
        Long projectId,
        String kitKey,
        String kitName,
        String kitVersion,
        StarterKitApplyCounts created,
        StarterKitApplyCounts skipped,
        List<String> warnings,
        String operatorName,
        LocalDateTime appliedAt
) {
}
