package com.dataspec.template.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 表模板字段
 */
@Data
@TableName("ds_template_field")
public class TemplateField {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属模板 */
    private Long templateId;

    /** 关联标准字段（可为空，表示自定义字段） */
    private Long fieldId;

    /** 字段名 */
    private String name;

    /** 数据类型 */
    private String dataType;

    /** 是否可空 */
    private Boolean nullable;

    /** 默认值 */
    private String defaultValue;

    /** 注释 */
    private String comment;

    /** 排序 */
    private Integer sortOrder;

    /** 是否必含 */
    private Boolean isRequired;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Boolean isDeleted;
}
