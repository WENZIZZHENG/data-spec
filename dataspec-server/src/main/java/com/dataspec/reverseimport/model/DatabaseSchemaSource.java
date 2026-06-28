package com.dataspec.reverseimport.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * schema dump 的非敏感来源信息，仅用于 AI 和测试判断来源边界。
 */
@Data
public class DatabaseSchemaSource {

    private String sourceType = "jdbc-metadata";

    private String databaseProductName;

    private String databaseProductVersion;

    private String catalogName;

    private String schemaName;

    private List<String> selectedTableNames = new ArrayList<>();

    private Integer tableCount = 0;
}
