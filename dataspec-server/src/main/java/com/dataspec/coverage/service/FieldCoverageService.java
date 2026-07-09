package com.dataspec.coverage.service;

import com.dataspec.coverage.model.FieldCoverageReport;
import com.dataspec.lint.model.TableDef;
import com.dataspec.reverseimport.model.DatabaseMetadataScanFailureSummary;
import com.dataspec.reverseimport.model.DatabaseMetadataScanPartialResult;

import java.util.List;

/**
 * 字段标准覆盖率报告服务。
 */
public interface FieldCoverageService {

    FieldCoverageReport reportSql(Long projectId, String sql);

    FieldCoverageReport reportTables(Long projectId, List<TableDef> tables);

    /**
     * 基于 metadata scan job 的 schema-only partial result 生成覆盖率；只统计 successful tables，
     * failed/skipped/not-yet-scanned 表必须在报告边界中标明且不得视为已覆盖。
     */
    FieldCoverageReport reportScanPartial(Long projectId,
                                          DatabaseMetadataScanPartialResult partialResult,
                                          DatabaseMetadataScanFailureSummary failureSummary,
                                          String scanStatus);
}
