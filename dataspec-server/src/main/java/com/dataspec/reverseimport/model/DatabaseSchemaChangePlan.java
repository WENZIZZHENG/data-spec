package com.dataspec.reverseimport.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 数据库 schema 变更计划预览。该响应只用于 dry-run 审计，不执行数据库迁移、不保存连接凭据。
 */
@Schema(description = "数据库 schema 变更计划预览；只用于 dry-run 审计，不执行数据库迁移、不保存连接凭据。")
@Data
public class DatabaseSchemaChangePlan {

    /** 响应类型标识，供 AI/CLI 判断 JSON 语义。 */
    @Schema(description = "响应类型标识，供 AI/CLI 判断 JSON 语义。")
    private String kind = "dataspec-database-schema-change-plan";

    /** 响应 schema 版本；新增可选字段时保持兼容递增策略。 */
    @Schema(description = "响应 schema 版本；新增可选字段时保持兼容递增策略。")
    private Integer schemaVersion = 1;

    /** DataSpec 项目 ID。 */
    @Schema(description = "DataSpec 项目 ID。")
    private Long projectId;

    /** 数据库类型，如 POSTGRESQL 或 MYSQL。 */
    @Schema(description = "数据库类型，如 POSTGRESQL 或 MYSQL。")
    private String databaseType;

    /** 数据库名；已脱敏，不包含 JDBC URL。 */
    @Schema(description = "数据库名；已脱敏，不包含 JDBC URL。")
    private String databaseName;

    /** schema 名；MySQL 场景可能为空或仅作为兼容输入。 */
    @Schema(description = "schema 名；PostgreSQL 用于限定对象，MySQL 场景可能为空或仅作为兼容输入。")
    private String schemaName;

    /** 当前源库 schema-only metadata hash，不包含密码、连接串或业务数据行。 */
    @Schema(description = "当前源库 schema-only metadata hash，不包含密码、连接串或业务数据行。")
    private String currentSchemaHash;

    /** 目标 DataSpec 标准摘要 hash，不包含连接凭据。 */
    @Schema(description = "目标 DataSpec 标准摘要 hash，不包含连接凭据。")
    private String targetSpecHash;

    /** 整体风险：SAFE、LOW、MEDIUM、HIGH 或 BLOCKED。 */
    @Schema(description = "整体风险：SAFE、LOW、MEDIUM、HIGH 或 BLOCKED。")
    private String riskLevel = "SAFE";

    /** 计划聚合统计。 */
    @Schema(description = "计划聚合统计，覆盖表、字段、风险和阻塞计数。")
    private DatabaseSchemaChangeSummary summary = new DatabaseSchemaChangeSummary();

    /** 字段级变更项列表。 */
    @Schema(description = "字段级变更项列表；每项描述一个 dry-run schema 差异处理建议。")
    private List<DatabaseSchemaChangeItem> changeSet = new ArrayList<>();

    /** 合并后的 dry-run SQL 草案；不得直接视为已审批迁移脚本。 */
    @Schema(description = "合并后的 dry-run SQL 草案；不得直接视为已审批迁移脚本。")
    private String migrationSql = "";

    /** 整体回滚提示。 */
    @Schema(description = "整体回滚提示；正式迁移前需要据此准备反向脚本或恢复方案。")
    private String rollbackHint = "";

    /** 全局人工检查点。 */
    @Schema(description = "全局人工检查点；正式迁移前必须逐项确认。")
    private List<String> manualChecks = new ArrayList<>();

    /** 全局阻塞原因；非空时客户端不得自动执行迁移。 */
    @Schema(description = "全局阻塞原因；非空时客户端不得自动执行迁移。")
    private List<String> blockedReasons = new ArrayList<>();

    /** 面向用户和 AI 的后续动作建议。 */
    @Schema(description = "面向用户和 AI 的后续动作建议。")
    private List<String> nextActions = new ArrayList<>();

    /** metadata cache 证据；只描述 schema-only 缓存状态，不包含凭据。 */
    @Schema(description = "metadata cache 证据；只描述 schema-only 缓存状态，不包含凭据。")
    private DatabaseMetadataCacheInfo metadataCache;
}
