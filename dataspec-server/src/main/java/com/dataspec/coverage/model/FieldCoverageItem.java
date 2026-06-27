package com.dataspec.coverage.model;

import lombok.Data;

/**
 * 单个数据库字段的标准覆盖情况。
 */
@Data
public class FieldCoverageItem {

    private String tableName;
    private String columnName;
    private String dataType;
    private String comment;
    private FieldCoverageStatus status;
    private boolean covered;
    private String standardFieldName;
    private String standardDisplayName;
    private String matchType;
    private String recommendedFieldName;
    private String reason;
}
