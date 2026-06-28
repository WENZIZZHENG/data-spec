package com.dataspec.activity.model;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 项目活动时间线条目。
 * <p>
 * metadata 只能包含安全摘要，不能放 SQL 原文、token hash/明文或数据库连接密码。
 */
public record ProjectActivityItem(
        String id,
        String actionType,
        String title,
        String description,
        LocalDateTime occurredAt,
        String actor,
        String source,
        String severity,
        String detailRoute,
        Map<String, Object> metadata
) {
}
