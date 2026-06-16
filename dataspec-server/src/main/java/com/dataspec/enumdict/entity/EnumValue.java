package com.dataspec.enumdict.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 枚举值
 */
@Data
@TableName("ds_enum_value")
public class EnumValue {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属枚举 */
    private Long enumId;

    /** 枚举值 */
    private String value;

    /** 显示标签 */
    private String label;

    /** 排序 */
    private Integer sortOrder;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Boolean isDeleted;
}
