package com.dataspec.coverage.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 表级字段覆盖率汇总和明细。
 */
@Data
public class FieldCoverageTable {

    private String tableName;
    private String comment;
    private int columnCount;
    private int coveredCount;
    private int unmanagedCount;
    private int missingCommentCount;
    private int possibleDuplicateCount;
    private double coverageRate;
    private List<FieldCoverageItem> fields = new ArrayList<>();
}
