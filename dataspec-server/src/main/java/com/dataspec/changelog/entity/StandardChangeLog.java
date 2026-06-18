package com.dataspec.changelog.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 标准变更记录。
 */
@Data
@TableName("ds_standard_change_log")
public class StandardChangeLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属项目 */
    private Long projectId;

    /** 目标类型: field/enum_dict/enum_value/rule_config */
    private String targetType;

    /** 目标记录 ID */
    private Long targetId;

    /** 动作: create/update/delete/toggle */
    private String action;

    /** 变更前 JSON 快照 */
    private String beforeJson;

    /** 变更后 JSON 快照 */
    private String afterJson;

    /** 变更时间 */
    private LocalDateTime changedAt;
}
