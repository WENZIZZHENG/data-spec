package com.dataspec.reverseimport.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * schema change plan 的单个字段级变更项。
 */
@Schema(description = "schema change plan 的单个字段级变更项；用于 API、CLI 和前端 dry-run 审计。")
@Data
public class DatabaseSchemaChangeItem {

    /** 来源数据库表名；来自 schema metadata，不包含连接串或业务数据行。 */
    @Schema(description = "来源数据库表名；来自 schema metadata，不包含连接串或业务数据行。")
    private String tableName;

    /** 来源数据库字段名；来自 schema metadata。 */
    @Schema(description = "来源数据库字段名；来自 schema metadata，不包含业务数据值。")
    private String columnName;

    /** 命中的 DataSpec 标准字段名；未命中标准时为空。 */
    @Schema(description = "命中的 DataSpec 标准字段名；未命中标准或删除候选时为空。")
    private String standardFieldName;

    /** 本变更的预览动作；第一版仅用于 dry-run 计划。 */
    @Schema(description = "本变更的预览动作；第一版仅用于 dry-run 计划，不代表自动执行。")
    private DatabaseSchemaChangeAction action;

    /** 发生变化的属性，如 dataType、nullable、defaultValue、comment 或 column。 */
    @Schema(description = "发生变化的属性，如 dataType、nullable、defaultValue、comment 或 column。")
    private String property;

    /** 当前数据库 metadata 值；已脱敏，不包含业务数据行。 */
    @Schema(description = "当前数据库 metadata 值；已脱敏，不包含业务数据行。")
    private String currentValue;

    /** 目标 DataSpec 标准值；已脱敏。 */
    @Schema(description = "目标 DataSpec 标准值；已脱敏，结构属性不会直接拼入可执行 SQL。")
    private String targetValue;

    /** 字段级风险：LOW、MEDIUM 或 HIGH。 */
    @Schema(description = "字段级风险：LOW、MEDIUM 或 HIGH；客户端用于突出人工确认优先级。")
    private String riskLevel;

    /** dry-run SQL 草案；高风险删除候选只输出注释，不输出可执行 DROP。 */
    @Schema(description = "dry-run SQL 草案；结构变更和高风险删除候选只输出 REVIEW/BLOCKED 注释，不输出可执行 DROP。")
    private String migrationSql;

    /** 回滚或撤销提示；用于 AI/用户生成正式迁移文件前检查。 */
    @Schema(description = "回滚或撤销提示；用于 AI/用户生成正式迁移文件前检查。")
    private String rollbackHint;

    /** 需要人工确认的检查点。 */
    @Schema(description = "需要人工确认的检查点；正式迁移前必须逐项确认。")
    private List<String> manualChecks = new ArrayList<>();

    /** 阻止自动执行的原因；存在值时客户端不得把该项当作可自动应用。 */
    @Schema(description = "阻止自动执行的原因；存在值时客户端不得把该项当作可自动应用。")
    private List<String> blockedReasons = new ArrayList<>();

    /** 对本项的可读解释。 */
    @Schema(description = "对本项的可读解释，说明为什么生成该计划项。")
    private String reason;
}
