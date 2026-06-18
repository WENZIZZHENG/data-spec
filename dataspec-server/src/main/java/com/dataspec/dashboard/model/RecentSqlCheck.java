package com.dataspec.dashboard.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 工作台最近 SQL 检查摘要。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecentSqlCheck {

    private Long id;
    private LocalDateTime createdAt;
    private Integer errorCount;
    private Integer warningCount;
    private Integer suggestionCount;
    private Integer issueCount;
}
