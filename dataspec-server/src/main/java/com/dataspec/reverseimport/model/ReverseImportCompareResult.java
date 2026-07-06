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
    /** 数据库直连二次比对关联的 metadata cache 证据；dump-only 场景可为空。 */
    private DatabaseMetadataCacheInfo metadataCache;
}
