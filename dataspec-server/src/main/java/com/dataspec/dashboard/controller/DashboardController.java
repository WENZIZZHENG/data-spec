package com.dataspec.dashboard.controller;

import com.dataspec.common.result.R;
import com.dataspec.dashboard.model.DashboardSummary;
import com.dataspec.dashboard.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 个人工作台 API。
 */
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/summary")
    public R<DashboardSummary> summary(@RequestParam Long projectId) {
        return R.ok(dashboardService.getSummary(projectId));
    }
}
