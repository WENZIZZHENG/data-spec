package com.dataspec.reverseimport.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 数据库结构离线 dump。该对象只保存 schema metadata，不保存密码、token、JDBC URL 或业务数据行。
 */
@Data
public class DatabaseSchemaDump {

    private String kind = "dataspec-database-schema-dump";

    private Integer schemaVersion = 1;

    private Long projectId;

    private String databaseType;

    private String databaseName;

    private String schemaName;

    private String generatedAt;

    private DatabaseSchemaSource source = new DatabaseSchemaSource();

    private List<DatabaseSchemaTable> tables = new ArrayList<>();

    private List<String> warnings = new ArrayList<>();
}
