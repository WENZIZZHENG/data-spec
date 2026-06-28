package com.dataspec.activity.controller;

import com.dataspec.activity.model.ProjectActivityTimeline;
import com.dataspec.activity.service.ProjectActivityService;
import com.dataspec.common.result.R;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 项目活动时间线 API。
 */
@RestController
@RequestMapping("/api/projects/{projectId}/activities")
@RequiredArgsConstructor
public class ProjectActivityController {

    private final ProjectActivityService projectActivityService;

    @GetMapping
    public R<ProjectActivityTimeline> list(
            @PathVariable Long projectId,
            @RequestParam(required = false) String actionType,
            @RequestParam(required = false) Integer limit) {
        return R.ok(projectActivityService.listActivities(projectId, actionType, limit));
    }
}
