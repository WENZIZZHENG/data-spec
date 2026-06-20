package com.dataspec.reverseimport.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 单表维度的数据库字段比对结果。
 */
@Data
public class ReverseImportTableDiff {

    private String tableName;
    private String comment;
    private List<ReverseImportFieldDiff> fieldDiffs = new ArrayList<>();
}
