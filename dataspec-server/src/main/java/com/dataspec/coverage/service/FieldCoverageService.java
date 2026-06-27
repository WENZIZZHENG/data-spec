package com.dataspec.coverage.service;

import com.dataspec.coverage.model.FieldCoverageReport;
import com.dataspec.lint.model.TableDef;

import java.util.List;

/**
 * 字段标准覆盖率报告服务。
 */
public interface FieldCoverageService {

    FieldCoverageReport reportSql(Long projectId, String sql);

    FieldCoverageReport reportTables(Long projectId, List<TableDef> tables);
}
