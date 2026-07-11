package com.dataspec.fieldsemantic.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 字段语义规则响应。
 *
 * @param id 语义规则 ID
 * @param projectId 所属项目 ID
 * @param fieldId 目标标准字段 ID
 * @param sourceFieldId 可选源字段 ID
 * @param ruleType 语义规则类型
 * @param unitConversion 单位换算说明
 * @param aggregationRule 聚合口径说明
 * @param timeGranularity 时间粒度说明
 * @param sourceOfTruth source of truth 或首选字段说明
 * @param recommendedUse 推荐使用场景
 * @param antiPatterns 常见误用或反例
 * @param evidenceRefs 证据引用列表
 * @param status 规则状态
 * @param createdAt 创建时间
 * @param updatedAt 更新时间
 */
@Schema(description = "字段语义规则响应。")
public record FieldSemanticRuleResp(
        Long id,
        Long projectId,
        Long fieldId,
        Long sourceFieldId,
        String ruleType,
        String unitConversion,
        String aggregationRule,
        String timeGranularity,
        String sourceOfTruth,
        String recommendedUse,
        String antiPatterns,
        List<String> evidenceRefs,
        String status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
