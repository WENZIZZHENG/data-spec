package com.dataspec.ruleexemption.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 项目级规则误报豁免。
 */
@Data
@TableName("ds_rule_exemption")
public class RuleExemption {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属项目 */
    private Long projectId;

    /** 被豁免的规则编码 */
    private String ruleCode;

    /** 豁免表名；为空表示不限表 */
    private String tableName;

    /** 豁免字段名；为空表示不限字段 */
    private String columnName;

    /** 豁免原因。必须说明历史兼容或第三方约束，避免把例外当成新标准。 */
    private String reason;

    /** 是否启用 */
    private Boolean enabled;

    /** 过期时间；为空表示不过期 */
    private LocalDateTime expiresAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Boolean isDeleted;
}
