package com.dataspec.reverseimport.service;

import com.dataspec.reverseimport.model.ReverseImportPreview;
import com.dataspec.reverseimport.model.DatabaseImportReq;
import com.dataspec.reverseimport.model.DatabaseImportResult;
import com.dataspec.lint.model.TableDef;

import java.util.List;

/**
 * SQL 反向导入预览服务。
 */
public interface ReverseImportService {

    ReverseImportPreview preview(Long projectId, String sql);

    ReverseImportPreview previewTables(Long projectId, List<TableDef> tables);

    DatabaseImportResult importCandidates(DatabaseImportReq req);
}
