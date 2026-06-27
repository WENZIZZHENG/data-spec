package com.dataspec.fieldquality.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 项目级标准字段质量报告。
 */
@Data
public class FieldQualityReport {

    private FieldQualitySummary summary = new FieldQualitySummary();
    private List<FieldQualityItem> fields = new ArrayList<>();
}
