package com.dataspec.reverseimport.model;

import lombok.Data;

/**
 * schema dump 中的索引列 metadata。该对象只描述索引结构，不包含业务数据行。
 */
@Data
public class DatabaseSchemaIndex {

    /** 索引所属 schema；MySQL 场景可能为空。 */
    private String schemaName;

    /** 索引所属表名。 */
    private String tableName;

    /** 数据库返回的索引名，可能因方言或驱动限制为空。 */
    private String indexName;

    /** 索引覆盖的列名；复合索引会按列返回多条记录。 */
    private String columnName;

    /** true 表示非唯一索引，false 表示唯一索引。 */
    private Boolean nonUnique;

    /** 复合索引中的列顺序，从 JDBC metadata 原值透传。 */
    private Integer ordinalPosition;
}
