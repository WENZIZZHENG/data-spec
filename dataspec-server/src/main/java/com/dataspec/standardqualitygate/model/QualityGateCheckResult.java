package com.dataspec.standardqualitygate.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 单项质量门禁检查结果，供前端、CLI 和 AI 读取。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QualityGateCheckResult {

    private String code;
    private String label;
    private String status;
    private String severity;
    private Double actualValue;
    private Double expectedValue;
    private String operator;
    private String message;
    private String route;
    private String nextAction;
}
