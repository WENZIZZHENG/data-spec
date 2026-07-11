package com.dataspec.tablemodel.model;

import java.util.List;

/**
 * 表级外键标准，使用结构化字段限制 DDL preview 的 SQL 片段来源。
 *
 * @param name 外键约束名，可为空由生成器派生
 * @param columns 本表列名
 * @param targetTable 目标表名
 * @param targetColumns 目标表列名
 * @param relationName 人可读关系名
 * @param onDelete 删除动作，支持受控值如 CASCADE、RESTRICT、SET NULL、NO ACTION
 * @param onUpdate 更新动作，支持受控值如 CASCADE、RESTRICT、SET NULL、NO ACTION
 * @param advisoryOnly 是否仅作为 AI/lint 提示，不拼入 DDL
 * @param notes 非敏感说明
 */
public record TableForeignKeyStandard(
        String name,
        List<String> columns,
        String targetTable,
        List<String> targetColumns,
        String relationName,
        String onDelete,
        String onUpdate,
        Boolean advisoryOnly,
        String notes
) {
}
