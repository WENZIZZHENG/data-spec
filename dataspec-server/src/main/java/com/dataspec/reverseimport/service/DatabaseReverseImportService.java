package com.dataspec.reverseimport.service;

import com.dataspec.coverage.model.FieldCoverageReport;
import com.dataspec.reverseimport.model.DatabaseConnectionReq;
import com.dataspec.reverseimport.model.DatabaseConnectionResult;
import com.dataspec.reverseimport.model.DatabaseCommentPatchPlan;
import com.dataspec.reverseimport.model.DatabaseMetadataBrowser;
import com.dataspec.reverseimport.model.DatabaseMetadataScanReq;
import com.dataspec.reverseimport.model.DatabaseMetadataScanResult;
import com.dataspec.reverseimport.model.DatabaseSchemaChangePlan;
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

    /**
     * 只读分页扫描数据库表级 metadata，返回 cursor/progress 供大库分批浏览；不保存凭据、不写源库或标准库。
     */
    DatabaseMetadataScanResult scan(DatabaseMetadataScanReq req);

    DatabaseSchemaDump exportDump(DatabaseConnectionReq req);

    /**
     * 只读读取所选表 schema metadata，并复用预览、比对和覆盖率逻辑生成候选浏览结果；不得写入源库或标准库。
     */
    DatabaseMetadataBrowser browse(DatabaseConnectionReq req);

    ReverseImportPreview preview(DatabaseConnectionReq req);

    ReverseImportPreview previewDump(DatabaseSchemaDumpReq req);

    ReverseImportCompareResult compare(DatabaseConnectionReq req);

    ReverseImportCompareResult compareDump(DatabaseSchemaDumpReq req);

    /**
     * 只读生成数据库 schema change plan，复用 schema metadata 与标准比对结果；不执行迁移、不写源库、不保存凭据。
     */
    DatabaseSchemaChangePlan planSchemaChange(DatabaseConnectionReq req);

    /**
     * 只读生成数据库 COMMENT 回写计划，复用 schema metadata、标准字段比对和表模板注释；不执行 SQL、不写源库或标准库。
     */
    DatabaseCommentPatchPlan planCommentPatch(DatabaseConnectionReq req);

    FieldCoverageReport coverage(DatabaseConnectionReq req);

    FieldCoverageReport coverageDump(DatabaseSchemaDumpReq req);
}
