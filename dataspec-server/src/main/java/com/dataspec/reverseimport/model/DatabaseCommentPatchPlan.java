package com.dataspec.reverseimport.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 数据库 COMMENT 回写计划预览。
 *
 * <p>该响应只用于 dry-run 审阅和 AI 证据导出，不执行源库写入、不写 DataSpec 字段库、不保存连接凭据。</p>
 */
@Schema(description = "数据库 COMMENT 回写计划预览；只输出 dry-run SQL 和审阅证据，不执行源库写入、不保存连接凭据。")
@Data
public class DatabaseCommentPatchPlan {

    /** 响应类型标识，供 CLI、前端和 AI 判断 JSON 语义。 */
    @Schema(description = "响应类型标识，供 CLI、前端和 AI 判断 JSON 语义。")
    private String kind = "dataspec-database-comment-patch-plan";

    /** 响应 schema 版本；新增可选字段时保持兼容递增。 */
    @Schema(description = "响应 schema 版本；新增可选字段时保持兼容递增。")
    private Integer schemaVersion = 1;

    /** DataSpec 项目 ID。 */
    @Schema(description = "DataSpec 项目 ID。")
    private Long projectId;

    /** 数据库类型，如 POSTGRESQL 或 MYSQL。 */
    @Schema(description = "数据库类型，如 POSTGRESQL 或 MYSQL。")
    private String databaseType;

    /** 数据库名；已脱敏，不包含 JDBC URL 或 DSN。 */
    @Schema(description = "数据库名；已脱敏，不包含 JDBC URL 或 DSN。")
    private String databaseName;

    /** schema 名；MySQL 场景可能为空。 */
    @Schema(description = "schema 名；PostgreSQL 用于限定对象，MySQL 场景可能为空。")
    private String schemaName;

    /** schema-only metadata fingerprint；不包含密码、连接串或业务数据行。 */
    @Schema(description = "schema-only metadata fingerprint；不包含密码、连接串或业务数据行。")
    private String metadataFingerprint;

    /** 当前 COMMENT 计划内容 hash；用于审阅、复制和后续人工迁移记录。 */
    @Schema(description = "当前 COMMENT 计划内容 hash；用于审阅、复制和后续人工迁移记录。")
    private String planHash;

    /** 计划聚合统计。 */
    @Schema(description = "计划聚合统计，覆盖表、字段、可执行变更、不支持项和阻塞项计数。")
    private DatabaseCommentPatchPlanSummary summary = new DatabaseCommentPatchPlanSummary();

    /** 表/字段 COMMENT 差异项。 */
    @Schema(description = "表/字段 COMMENT 差异项；每项描述当前注释、目标注释、风险、SQL 草稿和人工检查。")
    private List<DatabaseCommentPatchPlanItem> items = new ArrayList<>();

    /** 合并后的 dry-run SQL 草案；不得直接视为已审批迁移脚本。 */
    @Schema(description = "合并后的 dry-run SQL 草案；不得直接视为已审批迁移脚本。")
    private String dryRunSql = "";

    /** 当前方言对表/列 COMMENT SQL 的支持情况。 */
    @Schema(description = "当前方言对表/列 COMMENT SQL 的支持情况；客户端据此展示 unsupported 和人工处理建议。")
    private DatabaseCommentDialectSupport dialectSupport = new DatabaseCommentDialectSupport();

    /** 整体风险：SAFE、LOW、MEDIUM 或 HIGH。 */
    @Schema(description = "整体风险：SAFE、LOW、MEDIUM 或 HIGH；unsupported/blocked 会提升风险。")
    private String riskLevel = "SAFE";

    /** 整体回滚提示；正式迁移前需要据此准备反向 COMMENT。 */
    @Schema(description = "整体回滚提示；正式迁移前需要据此准备反向 COMMENT 或恢复方案。")
    private String rollbackHint = "";

    /** 计划生成证据；仅包含 schema-only 范围、标准引用和安全摘要。 */
    @Schema(description = "计划生成证据；仅包含 schema-only 范围、标准引用和安全摘要，不包含凭据或业务数据行。")
    private DatabaseCommentPatchPlanEvidence evidence = new DatabaseCommentPatchPlanEvidence();

    /** 只读安全边界。 */
    @Schema(description = "只读安全边界，说明本计划不会写源库、不会写 DataSpec 项目状态、可安全复制给 AI。")
    private DatabaseCommentPatchPlanSafety safety = new DatabaseCommentPatchPlanSafety();

    /** 面向用户和 AI 的后续动作建议。 */
    @Schema(description = "面向用户和 AI 的后续动作建议；不代表自动执行 SQL。")
    private List<String> nextActions = new ArrayList<>();
}
