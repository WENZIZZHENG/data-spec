package com.dataspec.reverseimport.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * COMMENT 回写计划的单个表或字段注释差异项。
 */
@Schema(description = "COMMENT 回写计划的单个表或字段注释差异项；用于 dry-run 审阅、前端展示和 AI 证据导出。")
@Data
public class DatabaseCommentPatchPlanItem {

    /** 对象类型：TABLE 或 COLUMN。 */
    @Schema(description = "对象类型：TABLE 或 COLUMN。")
    private String objectType;

    /** 来源 schema 名；MySQL 场景可能为空。 */
    @Schema(description = "来源 schema 名；MySQL 场景可能为空。")
    private String schemaName;

    /** 来源表名；来自 schema metadata，不包含连接串或业务数据行。 */
    @Schema(description = "来源表名；来自 schema metadata，不包含连接串或业务数据行。")
    private String tableName;

    /** 来源字段名；TABLE 项为空。 */
    @Schema(description = "来源字段名；TABLE 项为空，COLUMN 项来自 schema metadata。")
    private String columnName;

    /** 命中的 DataSpec 标准字段名；表项或未命中时为空。 */
    @Schema(description = "命中的 DataSpec 标准字段名；表项或未命中时为空。")
    private String standardFieldName;

    /** 差异状态：NO_OP、MISSING、CHANGED 或 UNSUPPORTED。 */
    @Schema(description = "差异状态：NO_OP、MISSING、CHANGED 或 UNSUPPORTED。")
    private String status;

    /** 当前数据库 COMMENT；已脱敏，不包含凭据或业务数据行。 */
    @Schema(description = "当前数据库 COMMENT；已脱敏，不包含凭据或业务数据行。")
    private String currentComment;

    /** 目标 DataSpec COMMENT；已脱敏。 */
    @Schema(description = "目标 DataSpec COMMENT；已脱敏，来源于表模板或标准字段注释。")
    private String targetComment;

    /** 面向用户和 AI 的注释差异说明。 */
    @Schema(description = "面向用户和 AI 的注释差异说明，例如缺失注释、旧值到新值或不支持原因。")
    private String commentDiff;

    /** 单项 dry-run SQL；unsupported/no-op 项为空。 */
    @Schema(description = "单项 dry-run SQL；unsupported/no-op 项为空，不代表已审批迁移脚本。")
    private String dryRunSql = "";

    /** 当前项的方言支持摘要。 */
    @Schema(description = "当前项的方言支持摘要，说明表/列 COMMENT 是否可安全生成 SQL。")
    private String dialectSupport;

    /** 字段级风险：LOW、MEDIUM 或 HIGH。 */
    @Schema(description = "字段级风险：LOW、MEDIUM 或 HIGH；unsupported 和人工确认项通常为 MEDIUM 以上。")
    private String riskLevel;

    /** 本项回滚提示。 */
    @Schema(description = "本项回滚提示；正式迁移前应据此准备反向 COMMENT。")
    private String rollbackHint;

    /** 证据引用，如 template:<tablePrefix> 或 field:<fieldName>。 */
    @Schema(description = "证据引用，如 template:<tablePrefix> 或 field:<fieldName>，不包含凭据。")
    private List<String> evidenceRefs = new ArrayList<>();

    /** 需要人工处理的检查点。 */
    @Schema(description = "需要人工处理的检查点；客户端不得把这些项当成自动执行。")
    private List<String> manualChecks = new ArrayList<>();

    /** 阻止生成可执行 SQL 的原因。 */
    @Schema(description = "阻止生成可执行 SQL 的原因；非空时 dryRunSql 应为空或仅为说明。")
    private List<String> blockedReasons = new ArrayList<>();
}
