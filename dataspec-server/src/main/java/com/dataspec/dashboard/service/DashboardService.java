package com.dataspec.dashboard.service;

import com.dataspec.dashboard.model.DashboardSummary;

/**
 * 个人工作台服务。
 */
public interface DashboardService {

    DashboardSummary getSummary(Long projectId);
}
