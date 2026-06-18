package com.dataspec.dashboard.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 个人工作台项目汇总。
 */
@Data
public class DashboardSummary {

    private Integer fieldCount;
    private Integer enumDictCount;
    private Integer ruleCount;
    private Integer forbiddenTermCount;
    private Long recentCheckCount;
    private Integer fieldHitRate;
    private List<RecentSqlCheck> recentChecks = new ArrayList<>();
    private List<IssueTrendPoint> trend = new ArrayList<>();
}
