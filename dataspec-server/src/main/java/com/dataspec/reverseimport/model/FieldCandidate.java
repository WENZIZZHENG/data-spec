package com.dataspec.reverseimport.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 从 SQL 解析得到的标准字段候选。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FieldCandidate {

    private String tableName;
    private String columnName;
    private String dataType;
    private Boolean nullable;
    private String defaultValue;
    private String comment;
}
