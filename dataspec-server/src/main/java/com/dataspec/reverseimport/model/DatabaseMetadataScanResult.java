package com.dataspec.reverseimport.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 数据库 metadata 分页扫描响应。该对象不包含密码、token、完整 JDBC URL 或源库业务数据行。
 */
@Data
public class DatabaseMetadataScanResult {

    /** 响应类型标识，便于 AI 和前端识别 payload。 */
    private String kind = "dataspec-database-metadata-scan";

    /** 扫描响应 schema 版本。 */
    private Integer schemaVersion = 1;

    /** 当前 DataSpec 项目 ID。 */
    private Long projectId;

    /** 数据库类型，如 POSTGRESQL 或 MYSQL。 */
    private String databaseType;

    /** 数据库名，已做敏感信息清洗。 */
    private String databaseName;

    /** schema 名；MySQL 场景可能为空。 */
    private String schemaName;

    /** 一轮扫描标识；不是服务端后台任务 ID，不承诺持久化生命周期。 */
    private String scanId;

    /** 当前连接可见表数量估算。 */
    private int estimatedTableCount;

    /** 下一批 cursor；为空表示没有后续批次。 */
    private String cursor;

    /** 当前页表级 metadata；不包含列 metadata 或业务数据行。 */
    private List<DatabaseTableInfo> tables = new ArrayList<>();

    /** 当前扫描页和下一批状态。 */
    private DatabaseMetadataScanProgress progress = new DatabaseMetadataScanProgress();

    /** 当前扫描页轻量汇总。 */
    private DatabaseMetadataScanSummary partialSummary = new DatabaseMetadataScanSummary();

    /** 面向 AI 的恢复提示；不得包含密码、token 或完整 JDBC URL。 */
    private String resumeCommand;

    /** true 表示用户已请求取消，不应继续请求下一批。 */
    private boolean cancelled;

    /** 扫描后的建议动作，不代表自动写入。 */
    private List<String> nextActions = new ArrayList<>();

    /** 当前扫描页关联的 metadata cache 证据，不包含凭据或业务数据行。 */
    private DatabaseMetadataCacheInfo metadataCache;
}
