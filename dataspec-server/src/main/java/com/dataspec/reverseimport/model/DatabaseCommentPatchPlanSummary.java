package com.dataspec.reverseimport.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * COMMENT 回写计划聚合统计。
 */
@Schema(description = "COMMENT 回写计划聚合统计；用于前端、CLI 和 AI 快速判断差异范围与风险。")
@Data
public class DatabaseCommentPatchPlanSummary {

    /** 本次计划覆盖的表数量。 */
    @Schema(description = "本次计划覆盖的表数量。")
    private int tableCount;

    /** 本次计划读取的字段数量。 */
    @Schema(description = "本次计划读取的字段数量。")
    private int columnCount;

    /** 计划项总数。 */
    @Schema(description = "计划项总数，包含 no-op、可执行变更和 unsupported 项。")
    private int itemCount;

    /** 可生成 dry-run SQL 的变更数量。 */
    @Schema(description = "可生成 dry-run SQL 的变更数量；不代表已执行或可自动执行。")
    private int executableChangeCount;

    /** 已一致、无需生成 COMMENT SQL 的项数量。 */
    @Schema(description = "已一致、无需生成 COMMENT SQL 的项数量。")
    private int noOpCount;

    /** 当前注释为空但存在目标标准注释的项数量。 */
    @Schema(description = "当前注释为空但存在目标标准注释的项数量。")
    private int missingCount;

    /** 当前注释与目标标准注释不同的项数量。 */
    @Schema(description = "当前注释与目标标准注释不同的项数量。")
    private int changedCount;

    /** 因方言或证据不足不能安全生成 SQL 的项数量。 */
    @Schema(description = "因方言或证据不足不能安全生成 SQL 的项数量。")
    private int unsupportedCount;

    /** 带阻塞原因、不得自动执行的项数量。 */
    @Schema(description = "带阻塞原因、不得自动执行的项数量。")
    private int blockedCount;
}
