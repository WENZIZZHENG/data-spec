package com.dataspec.reverseimport.model;

import lombok.Data;

/**
 * 数据库直连二次比对统计。
 */
@Data
public class ReverseImportCompareSummary {

    private Integer tableCount;
    private Integer columnCount;
    /** 命中标准字段名或别名的字段数，包含属性变化和缺注释字段。 */
    private Integer matchedCount;
    private Integer changedCount;
    private Integer newCount;
    private Integer missingCommentCount;
    private Integer nonStandardCount;
}
