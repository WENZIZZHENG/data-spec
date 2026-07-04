package com.dataspec.reverseimport.model;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 从 SQL 解析得到的标准字段候选。
 */
@Data
@NoArgsConstructor
public class FieldCandidate {

    private String tableName;
    private String columnName;
    private String dataType;
    private Boolean nullable;
    private String defaultValue;
    private String comment;
    private String decisionType;
    private Long matchedFieldId;
    private String matchedFieldName;
    private String matchReason;
    private Double confidence;
    private String ignoreReason;
    private String confirmReason;

    public FieldCandidate(String tableName,
                          String columnName,
                          String dataType,
                          Boolean nullable,
                          String defaultValue,
                          String comment) {
        this.tableName = tableName;
        this.columnName = columnName;
        this.dataType = dataType;
        this.nullable = nullable;
        this.defaultValue = defaultValue;
        this.comment = comment;
    }
}
