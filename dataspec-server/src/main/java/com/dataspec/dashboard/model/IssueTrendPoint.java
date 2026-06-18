package com.dataspec.dashboard.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 工作台 SQL 问题趋势点。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class IssueTrendPoint {

    private Long recordId;
    private LocalDateTime createdAt;
    private Integer issueCount;
    private Integer errorCount;
    private Integer warningCount;
    private Integer suggestionCount;
}
