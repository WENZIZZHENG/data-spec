package com.dataspec.reverseimport.model;

import lombok.Data;

/**
 * SQL 反向导入预览统计。
 */
@Data
public class ReverseImportSummary {

    private Integer tableCount;
    private Integer columnCount;
    private Integer candidateCount;
    private Integer missingCommentCount;
    private Integer nonStandardFieldCount;
}
