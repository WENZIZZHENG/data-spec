package com.dataspec.projectbackup.model;

public record ProjectBackupCounts(
        Integer domains,
        Integer fields,
        Integer enumDicts,
        Integer enumValues,
        Integer rules,
        Integer templates,
        Integer templateFields,
        Integer snapshots,
        Integer reverseImportBatches,
        Integer fieldSources,
        Integer changeLogs
) {
}
