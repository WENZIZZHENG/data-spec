package com.dataspec.fieldquality.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 单个字段质量问题。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FieldQualityIssue {

    private String code;
    private FieldQualitySeverity severity;
    private String message;
    private String suggestedAction;
    private int scorePenalty;
}
