package com.dataspec.importexport.model;

import lombok.Data;

/**
 * Excel 单个 Sheet 的导入预览统计。
 */
@Data
public class ExcelSheetSummary {

    private int total;
    private int createCount;
    private int updateCount;
    private int conflictCount;

    public void increaseTotal() {
        total++;
    }

    public void increaseCreateCount() {
        createCount++;
    }

    public void increaseUpdateCount() {
        updateCount++;
    }

    public void increaseConflictCount() {
        conflictCount++;
    }
}
