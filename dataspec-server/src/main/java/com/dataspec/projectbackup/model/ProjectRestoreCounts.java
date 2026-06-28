package com.dataspec.projectbackup.model;

public record ProjectRestoreCounts(
        Integer created,
        Integer updated,
        Integer skipped,
        Integer conflicts,
        Integer blocked,
        Integer warnings
) {
}
