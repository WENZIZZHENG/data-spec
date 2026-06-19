package com.dataspec.importexport.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Excel dry-run 行级明细，用于在写入前说明每行会新增、更新还是阻塞。
 */
@Data
public class ExcelImportPreviewItem {

    private String sheet;
    private Integer rowNumber;
    private String key;
    private String action;
    private String status;
    private String reason;
    private List<ExcelImportDiff> diffs = new ArrayList<>();
}
