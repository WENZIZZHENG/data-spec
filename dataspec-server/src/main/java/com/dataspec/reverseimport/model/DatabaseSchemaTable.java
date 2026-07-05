package com.dataspec.reverseimport.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * schema dump 中的表结构快照。
 */
@Data
public class DatabaseSchemaTable {

    private String schemaName;

    private String tableName;

    private String tableType;

    private String comment;

    private List<DatabaseSchemaColumn> columns = new ArrayList<>();

    /**
     * 表索引 metadata；只用于结构浏览和 AI schema-only 分析，不参与字段标准写入。
     */
    private List<DatabaseSchemaIndex> indexes = new ArrayList<>();

    private List<String> warnings = new ArrayList<>();
}
