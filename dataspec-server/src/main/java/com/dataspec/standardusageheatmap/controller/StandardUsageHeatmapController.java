package com.dataspec.standardusageheatmap.controller;

import com.dataspec.common.result.R;
import com.dataspec.standardusageheatmap.model.StandardUsageHeatmapReport;
import com.dataspec.standardusageheatmap.service.StandardUsageHeatmapService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 标准使用热区与清理优先级只读 API。
 */
@RestController
@RequestMapping("/api/standard-usage/heatmap")
@RequiredArgsConstructor
public class StandardUsageHeatmapController {

    private final StandardUsageHeatmapService standardUsageHeatmapService;

    /**
     * 查询项目标准使用热区报告。
     *
     * @param projectId 项目 ID，服务层会校验访问边界。
     * @return 标准使用热区报告。
     */
    @GetMapping
    public R<StandardUsageHeatmapReport> report(@RequestParam Long projectId) {
        return R.ok(standardUsageHeatmapService.report(projectId));
    }
}
