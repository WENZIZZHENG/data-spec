package com.dataspec.reverseimport.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 数据库 metadata 浏览器中的表级结构和字段分析结果。
 */
@Data
public class DatabaseMetadataBrowserTable {

    /** 表所属 schema；MySQL 场景可能为空。 */
    private String schemaName;

    /** 数据库表名。 */
    private String tableName;

    /** 数据库表类型，如 TABLE。 */
    private String tableType;

    /** 数据库表注释。 */
    private String comment;

    /** 表字段数量。 */
    private int columnCount;

    /** 表索引列 metadata 数量，复合索引按列计数。 */
    private int indexCount;

    /** 表内可导入候选数量。 */
    private int candidateCount;

    /** 表内缺注释字段数量。 */
    private int missingCommentCount;

    /** 表内属性变化字段数量。 */
    private int changedCount;

    /** 表内未纳管字段数量。 */
    private int unmanagedCount;

    /** 表索引 metadata。 */
    private List<DatabaseSchemaIndex> indexes = new ArrayList<>();

    /** 表字段浏览行。 */
    private List<DatabaseMetadataBrowserColumn> columns = new ArrayList<>();

    /** 方言或 JDBC metadata 读取过程中的非敏感警告。 */
    private List<String> warnings = new ArrayList<>();
}
