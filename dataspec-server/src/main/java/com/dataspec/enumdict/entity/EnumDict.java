package com.dataspec.enumdict.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 枚举字典
 */
@Data
@TableName("ds_enum_dict")
public class EnumDict {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属项目 */
    private Long projectId;

    /** 枚举名称 */
    private String name;

    /** 枚举编码 */
    private String code;

    /** 描述 */
    private String description;

    /** 值类型: integer, string */
    private String valueType;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Boolean isDeleted;
}
