package com.dataspec.reverseimport.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * schema change plan 的聚合统计。
 */
@Schema(description = "schema change plan 的聚合统计；用于前端和 AI 快速判断风险范围。")
@Data
public class DatabaseSchemaChangeSummary {

    /** 本次计划覆盖的表数量。 */
    @Schema(description = "本次计划覆盖的表数量。")
    private int tableCount;

    /** 本次计划读取的字段数量。 */
    @Schema(description = "本次计划读取的字段数量。")
    private int columnCount;

    /** 变更项数量。 */
    @Schema(description = "变更项数量。")
    private int changeCount;

    /** LOW 风险变更项数量。 */
    @Schema(description = "LOW 风险变更项数量。")
    private int lowRiskCount;

    /** MEDIUM 风险变更项数量。 */
    @Schema(description = "MEDIUM 风险变更项数量。")
    private int mediumRiskCount;

    /** HIGH 风险变更项数量。 */
    @Schema(description = "HIGH 风险变更项数量。")
    private int highRiskCount;

    /** 带阻塞原因、不得自动执行的变更项数量。 */
    @Schema(description = "带阻塞原因、不得自动执行的变更项数量。")
    private int blockedCount;
}
