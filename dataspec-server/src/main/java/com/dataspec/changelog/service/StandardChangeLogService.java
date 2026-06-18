package com.dataspec.changelog.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.dataspec.changelog.entity.StandardChangeLog;

/**
 * 标准变更记录服务。
 */
public interface StandardChangeLogService {

    String TARGET_FIELD = "field";
    String TARGET_ENUM_DICT = "enum_dict";
    String TARGET_ENUM_VALUE = "enum_value";
    String TARGET_RULE_CONFIG = "rule_config";

    String ACTION_CREATE = "create";
    String ACTION_UPDATE = "update";
    String ACTION_DELETE = "delete";
    String ACTION_TOGGLE = "toggle";

    /** 将实体当前状态序列化为 JSON 快照。 */
    String snapshot(Object value);

    /** 记录一次标准变更。 */
    void recordChange(Long projectId,
                      String targetType,
                      Long targetId,
                      String action,
                      String beforeJson,
                      String afterJson);

    IPage<StandardChangeLog> page(Long projectId, String targetType, Long targetId, int current, int size);
}
