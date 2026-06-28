package com.dataspec.lint.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * fixedSql 策略执行摘要。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FixPlanSummary {

    private int availableCount;
    private int appliedCount;
    private int plannedCount;
    private int skippedCount;
}
