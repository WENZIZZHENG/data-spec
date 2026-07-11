package com.dataspec.tablemodel.model;

import java.util.List;

/**
 * 表级索引标准。
 *
 * @param name 索引名，可为空由生成器派生
 * @param columns 索引列名数组
 * @param unique 是否唯一索引
 * @param method 索引方法，第一版仅允许 btree 或为空
 * @param notes 非敏感说明
 */
public record TableIndexStandard(
        String name,
        List<String> columns,
        Boolean unique,
        String method,
        String notes
) {
}
