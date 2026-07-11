package com.dataspec.tablemodel.model;

import java.util.List;

/**
 * 表模板上的结构标准，描述主键、唯一键、索引、外键和策略 guidance。
 *
 * @param businessObjectId 可选关联业务对象标准 ID
 * @param primaryKey 主键标准
 * @param uniqueKeys 唯一键标准数组
 * @param indexes 索引标准数组
 * @param foreignKeys 外键标准数组
 * @param checkHints CHECK 或校验提示，默认不拼 raw SQL
 * @param auditPolicy 审计字段策略
 * @param softDeletePolicy 软删除策略
 * @param dialectNotes 方言差异说明
 * @param aiUsageNotes AI 使用说明
 */
public record TableStructureStandard(
        Long businessObjectId,
        TablePrimaryKeyStandard primaryKey,
        List<TableUniqueKeyStandard> uniqueKeys,
        List<TableIndexStandard> indexes,
        List<TableForeignKeyStandard> foreignKeys,
        List<String> checkHints,
        TableAuditPolicy auditPolicy,
        TableSoftDeletePolicy softDeletePolicy,
        List<String> dialectNotes,
        String aiUsageNotes
) {
}
