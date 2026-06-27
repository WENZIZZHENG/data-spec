package com.dataspec.fieldconflict.model;

import lombok.Data;

@Data
public class FieldConflictSummary {
    private int totalFieldCount;
    private int conflictGroupCount;
    private int affectedFieldCount;
    private int errorCount;
    private int warningCount;
    private int infoCount;
    private int aliasConflictCount;
    private int semanticDuplicateCount;
    private int attributeMismatchCount;
}
