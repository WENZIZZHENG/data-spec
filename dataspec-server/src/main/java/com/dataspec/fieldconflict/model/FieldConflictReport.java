package com.dataspec.fieldconflict.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class FieldConflictReport {
    private Long projectId;
    private FieldConflictSummary summary = new FieldConflictSummary();
    private List<FieldConflictGroup> groups = new ArrayList<>();
}
