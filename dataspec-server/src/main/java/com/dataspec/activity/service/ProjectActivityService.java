package com.dataspec.activity.service;

import com.dataspec.activity.model.ProjectActivityTimeline;

/**
 * 项目活动时间线服务。
 */
public interface ProjectActivityService {

    ProjectActivityTimeline listActivities(Long projectId, String actionType, Integer limit);
}
