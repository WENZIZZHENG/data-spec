package com.dataspec.standardqualitygate.model;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 项目质量门禁评估结果。
 */
@Data
public class StandardQualityGateResult {

    private Long projectId;
    private Boolean enabled;
    private String status;
    private StandardQualityGateConfig config;
    private QualityGateSummary summary = new QualityGateSummary();
    private List<QualityGateCheckResult> checks = new ArrayList<>();
    private List<QualityGateCheckResult> failedChecks = new ArrayList<>();
    private List<String> nextActions = new ArrayList<>();
    private LocalDateTime evaluatedAt;
}
