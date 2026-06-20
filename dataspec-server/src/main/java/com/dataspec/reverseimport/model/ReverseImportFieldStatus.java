package com.dataspec.reverseimport.model;

/**
 * 数据库字段与 DataSpec 标准字段的比对状态。
 */
public enum ReverseImportFieldStatus {
    MATCHED,
    CHANGED,
    NEW,
    MISSING_COMMENT,
    NON_STANDARD
}
