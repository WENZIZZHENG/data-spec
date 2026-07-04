package com.dataspec.standardqualitygate.model;

import lombok.Data;

/**
 * 可选 SQL lint 摘要。由 CLI 或前端显式传入，不保存 SQL 原文。
 */
@Data
public class QualityGateLintSummary {

    private Integer errorCount;
    private Integer warningCount;
    private Integer suggestionCount;
}
