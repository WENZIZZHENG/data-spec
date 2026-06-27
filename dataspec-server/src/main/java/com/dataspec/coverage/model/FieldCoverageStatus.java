package com.dataspec.coverage.model;

/**
 * 数据库字段相对 DataSpec 标准字段库的覆盖状态。
 */
public enum FieldCoverageStatus {
    STANDARD_MATCH,
    ALIAS_MATCH,
    MISSING_COMMENT,
    POSSIBLE_DUPLICATE,
    UNMANAGED
}
