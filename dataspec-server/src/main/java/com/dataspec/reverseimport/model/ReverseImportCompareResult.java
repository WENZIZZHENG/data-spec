package com.dataspec.reverseimport.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 数据库直连反向导入二次比对结果。
 */
@Data
public class ReverseImportCompareResult {

    private ReverseImportCompareSummary summary = new ReverseImportCompareSummary();
    private List<ReverseImportTableDiff> tableDiffs = new ArrayList<>();
}
