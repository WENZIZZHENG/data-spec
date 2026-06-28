package com.dataspec.reverseimport.service;

import com.dataspec.coverage.model.FieldCoverageReport;
import com.dataspec.reverseimport.model.DatabaseConnectionReq;
import com.dataspec.reverseimport.model.DatabaseConnectionResult;
import com.dataspec.reverseimport.model.DatabaseSchemaDump;
import com.dataspec.reverseimport.model.DatabaseSchemaDumpReq;
import com.dataspec.reverseimport.model.DatabaseTableInfo;
import com.dataspec.reverseimport.model.ReverseImportCompareResult;
import com.dataspec.reverseimport.model.ReverseImportPreview;

import java.util.List;

/**
 * 数据库直连反向导入服务。
 */
public interface DatabaseReverseImportService {

    DatabaseConnectionResult testConnection(DatabaseConnectionReq req);

    List<DatabaseTableInfo> listTables(DatabaseConnectionReq req);

    DatabaseSchemaDump exportDump(DatabaseConnectionReq req);

    ReverseImportPreview preview(DatabaseConnectionReq req);

    ReverseImportPreview previewDump(DatabaseSchemaDumpReq req);

    ReverseImportCompareResult compare(DatabaseConnectionReq req);

    ReverseImportCompareResult compareDump(DatabaseSchemaDumpReq req);

    FieldCoverageReport coverage(DatabaseConnectionReq req);

    FieldCoverageReport coverageDump(DatabaseSchemaDumpReq req);
}
