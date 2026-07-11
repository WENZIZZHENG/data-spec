package com.dataspec.tablemodel.model;

import java.util.List;

/**
 * 审计字段策略，只作为 DDL/AI guidance，不自动创建或回写字段。
 *
 * @param requiredFields 推荐或必含的审计字段名
 * @param createdAtField 创建时间字段名
 * @param updatedAtField 更新时间字段名
 * @param createdByField 创建人字段名
 * @param updatedByField 更新人字段名
 * @param notes 非敏感说明
 */
public record TableAuditPolicy(
        List<String> requiredFields,
        String createdAtField,
        String updatedAtField,
        String createdByField,
        String updatedByField,
        String notes
) {
}
