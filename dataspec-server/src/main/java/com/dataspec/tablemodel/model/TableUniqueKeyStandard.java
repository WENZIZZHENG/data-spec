package com.dataspec.tablemodel.model;

import java.util.List;

/**
 * 表级唯一键标准。
 *
 * @param name 唯一约束名，可为空由生成器派生
 * @param columns 参与唯一约束的列名数组
 * @param notes 非敏感说明
 */
public record TableUniqueKeyStandard(
        String name,
        List<String> columns,
        String notes
) {
}
