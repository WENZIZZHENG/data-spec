package com.dataspec.tablemodel.model;

import java.util.List;

/**
 * 表级主键标准。
 *
 * @param name 主键约束名，可为空由生成器派生
 * @param columns 主键列名数组
 * @param notes 非敏感说明
 */
public record TablePrimaryKeyStandard(
        String name,
        List<String> columns,
        String notes
) {
}
