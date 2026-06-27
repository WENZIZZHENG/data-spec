package com.dataspec.fieldquality.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 单个标准字段的质量评分结果。
 */
@Data
public class FieldQualityItem {

    private Long fieldId;
    private String name;
    private String displayName;
    private String dataType;
    private String status;
    private Boolean sensitive;
    private Long codeSetId;
    private int score;
    private FieldQualityLevel level;
    private List<FieldQualityIssue> issues = new ArrayList<>();
    private List<String> suggestions = new ArrayList<>();
}
