package com.dataspec.reverseimport.model;

import lombok.Data;

/**
 * schema dump 中的列结构快照，只描述列 metadata。
 */
@Data
public class DatabaseSchemaColumn {

    private String columnName;

    private String dataType;

    private Boolean nullable;

    private String defaultValue;

    private String comment;

    private Integer ordinalPosition;
}
