package com.dataspec.projectbackup.model;

public record ProjectRestoreItem(
        String assetType,
        String key,
        String action,
        String reason
) {
}
