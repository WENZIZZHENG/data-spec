package com.dataspec.field.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 标准字段
 */
@Data
@TableName("ds_field")
public class Field {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属项目 */
    private Long projectId;

    /** 字段名（snake_case） */
    private String name;

    /** 显示名称 */
    private String displayName;

    /** 数据类型: varchar, bigint, timestamp with time zone 等 */
    private String dataType;

    /** 长度 */
    private Integer length;

    /** 精度 */
    @TableField("precision_val")
    private Integer precisionVal;

    /** 小数位 */
    @TableField("scale_val")
    private Integer scaleVal;

    /** 是否可空 */
    private Boolean nullable;

    /** 默认值 */
    private String defaultValue;

    /** 注释 */
    private String comment;

    /** 所属数据域 */
    private Long domainId;

    /** 标签，逗号分隔 */
    private String tags;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Boolean isDeleted;
}
