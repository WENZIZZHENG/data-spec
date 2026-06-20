package com.dataspec.reverseimport.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 单个数据库字段的标准比对明细。
 */
@Data
public class ReverseImportFieldDiff {

    private String tableName;
    private String columnName;
    private String dataType;
    private Boolean nullable;
    private String defaultValue;
    private String comment;
    private String standardFieldName;
    private String standardDisplayName;
    private ReverseImportFieldStatus status;
    private String reason;
    private Boolean nonStandard = false;
    private List<ReverseImportFieldChange> changes = new ArrayList<>();
}
