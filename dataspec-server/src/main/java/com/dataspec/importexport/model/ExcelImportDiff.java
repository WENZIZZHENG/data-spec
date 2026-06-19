package com.dataspec.importexport.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Excel dry-run 字段级差异。beforeValue 为空表示新增提交值。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExcelImportDiff {

    private String field;
    private String beforeValue;
    private String afterValue;
}
