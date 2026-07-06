package com.dataspec.standardusageheatmap.service;

import com.dataspec.standardusageheatmap.model.StandardUsageHeatmapReport;

/**
 * 标准使用热区只读聚合服务。
 */
public interface StandardUsageHeatmapService {

    /**
     * 生成项目字段使用热区与清理优先级报告。
     *
     * @param projectId 项目 ID，必须有当前调用方访问权限。
     * @return 只读热区报告，不包含 SQL 原文、AI payload 或 raw source metadata。
     */
    StandardUsageHeatmapReport report(Long projectId);
}
