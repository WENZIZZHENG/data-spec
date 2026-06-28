package com.dataspec.standardchange.model;

import com.dataspec.standard.dto.StandardSnapshotInfo;

import java.util.List;

/**
 * 标准变更保存前 what-if 预览结果。
 */
public record StandardChangePreview(
        Long projectId,
        String targetType,
        Long targetId,
        String targetName,
        String operation,
        String riskLevel,
        boolean requiresConfirmation,
        String summary,
        List<StandardChangePreviewChange> changes,
        List<StandardChangePreviewImpact> impacts,
        List<String> validationCommands,
        List<StandardChangeRollbackHint> rollbackHints,
        StandardSnapshotInfo currentSnapshot
) {
}
