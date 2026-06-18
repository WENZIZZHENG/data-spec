package com.dataspec.reverseimport.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 反向导入发现的注释缺失项。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MissingCommentIssue {

    private String tableName;
    private String columnName;
    private String targetType;
}
