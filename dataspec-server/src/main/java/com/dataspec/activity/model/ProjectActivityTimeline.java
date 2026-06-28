package com.dataspec.activity.model;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 项目活动时间线响应。
 */
public record ProjectActivityTimeline(
        Long projectId,
        List<ProjectActivityAction> availableActionTypes,
        List<ProjectActivityItem> activities,
        LocalDateTime generatedAt
) {
}
