package com.dataspec.fieldimpact.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 单个标准字段的项目内影响分析结果。
 */
@Data
public class FieldImpactReport {

    private Long projectId;
    private Long fieldId;
    private String fieldName;
    private String displayName;
    private FieldImpactSummary summary = new FieldImpactSummary();
    private List<FieldImpactItem> impacts = new ArrayList<>();
    private List<FieldEditWarning> editWarnings = new ArrayList<>();
}
