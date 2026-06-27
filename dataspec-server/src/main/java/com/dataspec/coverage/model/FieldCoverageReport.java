package com.dataspec.coverage.model;

import lombok.Data;

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
}
