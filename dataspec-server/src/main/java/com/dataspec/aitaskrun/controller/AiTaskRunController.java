package com.dataspec.aitaskrun.controller;

import com.dataspec.aitaskrun.model.AiTaskRunDetail;
import com.dataspec.aitaskrun.model.AiTaskRunListItem;
import com.dataspec.aitaskrun.service.AiTaskRunService;
import com.dataspec.common.result.PageResult;
import com.dataspec.common.result.R;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * AI 任务运行状态只读 API。
 */
@RestController
@RequestMapping("/api/ai-task-runs")
@RequiredArgsConstructor
public class AiTaskRunController {

    private final AiTaskRunService aiTaskRunService;

    @GetMapping
    public R<PageResult<AiTaskRunListItem>> list(
            @RequestParam Long projectId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String taskType,
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size
    ) {
        return R.ok(aiTaskRunService.list(projectId, status, taskType, current, size));
    }

    @GetMapping("/recent-failures")
    public R<List<AiTaskRunListItem>> recentFailures(
            @RequestParam Long projectId,
            @RequestParam(defaultValue = "10") Integer limit
    ) {
        return R.ok(aiTaskRunService.recentFailures(projectId, limit));
    }

    @GetMapping("/{id}")
    public R<AiTaskRunDetail> detail(@PathVariable Long id, @RequestParam Long projectId) {
        return R.ok(aiTaskRunService.detail(projectId, id));
    }
}
