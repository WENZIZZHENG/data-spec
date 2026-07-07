package com.dataspec.standardevidence.model;

/**
 * 跨来源证据视图的目标标准对象摘要。
 *
 * @param subjectType 目标对象类型，第一版固定为 FIELD。
 * @param subjectId 目标对象 ID，FIELD 时为标准字段 ID。
 * @param name 标准对象技术名或字段名。
 * @param displayName 面向业务的显示名称，可为空。
 * @param dataType FIELD 对象的数据类型，可为空。
 * @param status 标准对象生命周期状态，如 enabled、draft、deprecated、disabled。
 */
public record StandardEvidenceSubject(
        String subjectType,
        Long subjectId,
        String name,
        String displayName,
        String dataType,
        String status
) {
}
