package com.dataspec.importexport.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Excel 导入预览错误，使用 Excel 行号方便用户回表定位。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExcelImportError {

    private String sheet;
    private Integer rowNumber;
    private String field;
    private String message;
}
