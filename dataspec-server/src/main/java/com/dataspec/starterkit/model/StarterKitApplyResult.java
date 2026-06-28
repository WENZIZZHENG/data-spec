package com.dataspec.starterkit.model;

import java.time.LocalDateTime;
import java.util.List;

public record StarterKitApplyResult(
        Long projectId,
        String kitKey,
        String kitName,
        String kitVersion,
        StarterKitApplyCounts created,
        StarterKitApplyCounts skipped,
        List<String> createdFields,
        List<String> skippedFields,
        List<String> createdEnums,
        List<String> skippedEnums,
        List<String> createdTemplates,
        List<String> skippedTemplates,
        List<String> warnings,
        LocalDateTime appliedAt
) {
}
