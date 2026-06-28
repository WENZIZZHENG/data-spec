package com.dataspec.projectbackup.model;

import java.time.LocalDateTime;

public record BackupProject(
        Long id,
        String name,
        String description,
        String dbType,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
