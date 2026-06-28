package com.dataspec.projectbackup.model;

import java.util.List;

public record ProjectBackupSanitization(
        Boolean safe,
        List<String> removedFields,
        List<String> warnings
) {
}
