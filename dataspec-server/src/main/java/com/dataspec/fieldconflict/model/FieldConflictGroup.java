package com.dataspec.fieldconflict.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class FieldConflictGroup {
    private String groupKey;
    private FieldConflictType conflictType;
    private FieldConflictSeverity severity;
    private String title;
    private String description;
    private List<FieldConflictField> fields = new ArrayList<>();
    private List<String> evidence = new ArrayList<>();
    private String suggestedAction;
}
