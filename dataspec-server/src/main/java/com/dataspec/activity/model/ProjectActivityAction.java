package com.dataspec.activity.model;

/**
 * 项目活动类型定义，供前端筛选使用。
 */
public record ProjectActivityAction(
        String actionType,
        String label
) {
}
