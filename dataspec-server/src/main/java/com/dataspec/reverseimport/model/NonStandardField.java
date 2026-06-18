package com.dataspec.reverseimport.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 反向导入发现的非标准字段。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NonStandardField {

    private String tableName;
    private String columnName;
    private String dataType;
    private String recommendedName;
    private String reason;
}
