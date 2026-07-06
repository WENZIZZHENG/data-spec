package com.dataspec.coverage.model;

import lombok.Data;

import com.dataspec.reverseimport.model.DatabaseMetadataCacheInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * 项目字段标准覆盖率报告。
 */
@Data
public class FieldCoverageReport {

    private FieldCoverageSummary summary = new FieldCoverageSummary();
    private List<FieldCoverageTable> tables = new ArrayList<>();
    private List<UnmanagedFieldRanking> unmanagedRankings = new ArrayList<>();
    /** 数据库直连覆盖率报告关联的 metadata cache 证据；SQL 或 dump 输入场景可为空。 */
    private DatabaseMetadataCacheInfo metadataCache;
}
