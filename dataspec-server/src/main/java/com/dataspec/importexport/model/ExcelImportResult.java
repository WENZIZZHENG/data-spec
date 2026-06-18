package com.dataspec.importexport.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Excel 确认导入结果。若预览无效，则不写入并返回错误列表。
 */
@Data
public class ExcelImportResult {

    private int importedFields;
    private int importedEnumDicts;
    private int importedEnumValues;
    private List<ExcelImportError> errors = new ArrayList<>();

    public Boolean getSuccess() {
        return errors == null || errors.isEmpty();
    }
}
