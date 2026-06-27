package com.dataspec.lint.model;

import com.dataspec.standard.dto.StandardSnapshotInfo;

import java.util.List;

/**
 * SQL 检查记录的标准快照回放信息。
 */
public record SqlCheckReplay(
        StandardSnapshotInfo recordedStandard,
        StandardSnapshotInfo currentStandard,
        String status,
        Summary summary,
        List<String> nextActions
) {

    public record Summary(
            boolean sameAsCurrent,
            int fieldCount,
            int enumCount,
            int ruleCount,
            String exportCommand
    ) {
    }
}
