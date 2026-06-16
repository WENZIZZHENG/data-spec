package com.dataspec.rule.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 规则配置
 */
@Data
@TableName("ds_rule_config")
public class RuleConfig {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属项目 */
    private Long projectId;

    /** 规则编码（对应 LintRule 实现类标识） */
    private String ruleCode;

    /** 规则名称 */
    private String ruleName;

    /** 严重级别: error, warning, suggestion */
    private String severity;

    /** 是否启用 */
    private Boolean enabled;

    /** 规则参数（JSON） */
    private String paramsJson;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Boolean isDeleted;
}
