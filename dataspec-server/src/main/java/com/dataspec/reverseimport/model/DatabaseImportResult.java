package com.dataspec.reverseimport.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 字段候选确认导入结果。
 */
@Data
public class DatabaseImportResult {

    private int importedCount;
    private int skippedCount;
    private List<String> importedFields = new ArrayList<>();
    private List<String> skippedFields = new ArrayList<>();
}
