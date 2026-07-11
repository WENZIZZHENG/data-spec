package com.dataspec.template.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 表模板
 */
@Data
@TableName("ds_template")
public class Template {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属项目 */
    private Long projectId;

    /** 模板名称 */
    private String name;

    /** 描述 */
    private String description;

    /** 表名前缀 */
    private String tablePrefix;

    /** 可选关联业务对象标准 ID */
    private Long businessObjectId;

    /** 主键标准 JSON，描述 constraintName 和 columns */
    private String primaryKeyJson;

    /** 唯一键标准数组 JSON */
    private String uniqueKeysJson;

    /** 索引标准数组 JSON */
    private String indexesJson;

    /** 外键标准数组 JSON */
    private String foreignKeysJson;

    /** CHECK 或校验提示 JSON，默认只作为 guidance */
    private String checkHintsJson;

    /** 审计字段策略 JSON */
    private String auditPolicyJson;

    /** 软删除策略 JSON */
    private String softDeletePolicyJson;

    /** 方言差异说明 JSON */
    private String dialectNotesJson;

    /** AI 使用说明，不得包含凭据或业务数据行 */
    private String aiUsageNotes;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Boolean isDeleted;
}
