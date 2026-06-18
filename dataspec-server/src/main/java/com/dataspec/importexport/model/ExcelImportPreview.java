package com.dataspec.importexport.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Excel 导入预览结果，不写数据库，只展示新增、更新和错误。
 */
@Data
public class ExcelImportPreview {

    private Boolean valid = true;
    private ExcelSheetSummary fields = new ExcelSheetSummary();
    private ExcelSheetSummary enumDicts = new ExcelSheetSummary();
    private ExcelSheetSummary enumValues = new ExcelSheetSummary();
    private List<ExcelImportError> errors = new ArrayList<>();

    public void addError(String sheet, Integer rowNumber, String field, String message) {
        valid = false;
        errors.add(new ExcelImportError(sheet, rowNumber, field, message));
    }
}
