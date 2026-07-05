package com.dataspec.reverseimport.model;

import com.dataspec.coverage.model.FieldCoverageReport;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 数据库直连 metadata 浏览器响应。该对象只包含 schema metadata 和标准分析结果，不包含密码、JDBC URL 或业务数据行。
 */
@Data
public class DatabaseMetadataBrowser {

    /** 响应类型标识，便于 AI 和前端识别 payload。 */
    private String kind = "dataspec-database-metadata-browser";

    /** 浏览器响应 schema 版本。 */
    private Integer schemaVersion = 1;

    /** 当前 DataSpec 项目 ID。 */
    private Long projectId;

    /** 数据库类型，如 POSTGRESQL 或 MYSQL。 */
    private String databaseType;

    /** 数据库名，已做敏感信息清洗。 */
    private String databaseName;

    /** schema 名；MySQL 场景可能为空。 */
    private String schemaName;

    /** 本次用户选择浏览的表名。 */
    private List<String> selectedTableNames = new ArrayList<>();

    /** 结构浏览和标准分析汇总。 */
    private DatabaseMetadataBrowserSummary summary = new DatabaseMetadataBrowserSummary();

    /** 表级 metadata 浏览结果。 */
    private List<DatabaseMetadataBrowserTable> tables = new ArrayList<>();

    /** 面向 AI 交接的 schema-only 文本摘要。 */
    private String aiReadableSummary;

    /** 浏览后的建议动作，不代表自动写入。 */
    private List<String> nextActions = new ArrayList<>();

    /** 复用既有反向导入预览结果，供前端候选导入流程继续使用。 */
    private ReverseImportPreview preview;

    /** 复用既有标准差异比对结果，供前端展示类型差异等状态。 */
    private ReverseImportCompareResult compare;

    /** 复用既有字段覆盖率报告，供 AI 或前端继续生成覆盖率分析。 */
    private FieldCoverageReport coverage;
}
