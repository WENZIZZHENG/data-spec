package com.dataspec.metric.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * 指标口径保存请求。
 *
 * @param projectId 所属项目 ID，创建时必填；更新时若提供则必须与原记录一致
 * @param metricKey 项目内唯一指标键，建议使用 snake_case
 * @param displayName 指标展示名称
 * @param definition 指标业务定义文本
 * @param measureFieldIds 度量字段 ID 列表，字段必须属于同一项目
 * @param dimensionFieldIds 维度字段 ID 列表，字段必须属于同一项目
 * @param filterRule 指标过滤口径说明，不自动改写 SQL
 * @param aggregationRule 指标聚合口径说明
 * @param timeGrain 指标默认时间粒度
 * @param ownerNotes 维护者说明或取舍记录
 * @param exampleSql 示例 SQL，仅作说明和 AI guidance，不会被执行
 * @param evidenceRefs 证据引用列表
 * @param status 指标口径状态
 */
@Schema(description = "指标口径保存请求。")
public record MetricDefinitionReq(
        @NotNull(message = "项目ID不能为空")
        @Schema(description = "所属项目 ID，创建时必填；更新时若提供则必须与原记录一致。")
        Long projectId,
        @NotBlank(message = "指标键不能为空")
        @Schema(description = "项目内唯一指标键，建议使用 snake_case。")
        String metricKey,
        @NotBlank(message = "指标展示名称不能为空")
        @Schema(description = "指标展示名称。")
        String displayName,
        @NotBlank(message = "指标定义不能为空")
        @Schema(description = "指标业务定义文本。")
        String definition,
        @Schema(description = "度量字段 ID 列表，字段必须属于同一项目。")
        List<Long> measureFieldIds,
        @Schema(description = "维度字段 ID 列表，字段必须属于同一项目。")
        List<Long> dimensionFieldIds,
        @Schema(description = "指标过滤口径说明，不自动改写 SQL。")
        String filterRule,
        @Schema(description = "指标聚合口径说明。")
        String aggregationRule,
        @Schema(description = "指标默认时间粒度。")
        String timeGrain,
        @Schema(description = "维护者说明或取舍记录；不得包含凭据或业务数据行。")
        String ownerNotes,
        @Schema(description = "示例 SQL，仅作说明和 AI guidance，不会被执行。")
        String exampleSql,
        @Schema(description = "证据引用列表。")
        List<String> evidenceRefs,
        @Schema(description = "指标口径状态，enabled 表示默认进入知识卡和 AI Context。")
        String status
) {
}
