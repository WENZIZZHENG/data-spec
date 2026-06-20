package com.dataspec.reverseimport.service;

import com.dataspec.reverseimport.model.DatabaseConnectionReq;
import com.dataspec.reverseimport.model.DatabaseConnectionResult;
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

    ReverseImportPreview preview(DatabaseConnectionReq req);

    ReverseImportCompareResult compare(DatabaseConnectionReq req);
}
