package com.dataspec.dialect.model;

/**
 * 方言诊断能力维度，供 API/CLI/AI 用稳定枚举识别能力边界。
 */
public enum DialectCapability {
    DIALECT_DETECTION,
    COMMENTS,
    AUTO_INCREMENT,
    INDEXES_CONSTRAINTS,
    TYPE_MAPPING,
    SCHEMA_CATALOG,
    QUOTED_IDENTIFIER,
    FIXED_SQL,
    DDL_GENERATION,
    DATABASE_METADATA
}
