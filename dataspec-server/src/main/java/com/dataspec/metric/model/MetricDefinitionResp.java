package com.dataspec.metric.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 指标口径响应。
 *
 * @param id 指标口径 ID
 * @param projectId 所属项目 ID
 * @param metricKey 项目内唯一指标键
 * @param displayName 指标展示名称
 * @param definition 指标业务定义文本
 * @param measureFieldIds 度量字段 ID 列表
 * @param dimensionFieldIds 维度字段 ID 列表
 * @param filterRule 指标过滤口径说明
 * @param aggregationRule 指标聚合口径说明
 * @param timeGrain 指标默认时间粒度
 * @param ownerNotes 维护者说明或取舍记录
 * @param exampleSql 示例 SQL，仅作说明和 AI guidance
 * @param evidenceRefs 证据引用列表
 * @param status 指标口径状态
 * @param createdAt 创建时间
 * @param updatedAt 更新时间
 */
@Schema(description = "指标口径响应。")
public record MetricDefinitionResp(
        Long id,
        Long projectId,
        String metricKey,
        String displayName,
        String definition,
        List<Long> measureFieldIds,
        List<Long> dimensionFieldIds,
        String filterRule,
        String aggregationRule,
        String timeGrain,
        String ownerNotes,
        String exampleSql,
        List<String> evidenceRefs,
        String status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
