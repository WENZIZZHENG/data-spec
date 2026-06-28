package com.dataspec.lint.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * fixedSql 候选 SQL 与修复计划。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FixedSqlPlan {

    private FixPolicy fixPolicy;
    private Boolean fixDryRun;
    private String fixedSql;
    private List<FixChange> fixChanges;
    private List<FixChange> fixExplanations;
    private FixPlanSummary fixSummary;
    private List<String> fixNextActions;
}
