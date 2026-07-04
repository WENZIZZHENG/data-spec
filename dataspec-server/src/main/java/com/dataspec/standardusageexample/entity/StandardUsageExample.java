package com.dataspec.standardusageexample.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 项目级标准字段/规则/模板使用示例。示例用于 AI Context，不允许保存真实业务行或 secret。
 */
@Data
@TableName("ds_standard_usage_example")
public class StandardUsageExample {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long projectId;
    private Long fieldId;
    private String ruleCode;
    private Long templateId;
    private String scope;
    private String exampleType;
    private String input;
    private String expectedOutput;
    private String antiPattern;
    private String reason;
    private String tags;
    private Integer priority;
    private String status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Boolean isDeleted;
}
