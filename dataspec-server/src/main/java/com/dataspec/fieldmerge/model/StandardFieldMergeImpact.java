package com.dataspec.fieldmerge.model;

import java.util.Map;

/**
 * 标准字段合并影响对象。
 *
 * @param impactType 影响类型，如 FIELD_LIFECYCLE、FIELD_SOURCE、AI_CONTEXT。
 * @param sourceId   影响对象 ID，可为空。
 * @param title      影响对象标题。
 * @param count      影响数量或摘要计数。
 * @param description 脱敏后的影响说明。
 * @param metadata   结构化补充信息，不能包含凭据或源库行值。
 */
public record StandardFieldMergeImpact(
        String impactType,
        Long sourceId,
        String title,
        int count,
        String description,
        Map<String, Object> metadata
) {
}
