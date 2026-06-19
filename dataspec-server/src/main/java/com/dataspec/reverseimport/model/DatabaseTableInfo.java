package com.dataspec.reverseimport.model;

/**
 * 数据库表信息。
 */
public record DatabaseTableInfo(String schemaName, String tableName, String tableType, String comment) {
}
