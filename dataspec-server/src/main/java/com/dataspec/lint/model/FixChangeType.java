package com.dataspec.lint.model;

/**
 * fixedSql 变更类型，供前端和 AI 按类别解释风险。
 */
public enum FixChangeType {
    TABLE_RENAME,
    COLUMN_RENAME,
    REQUIRED_COLUMN_ADD,
    UNSUPPORTED_RULE,
    UNSAFE_REBUILD
}
