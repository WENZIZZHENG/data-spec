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

    /** 别名，逗号分隔，用于 AI 按自然语言或历史字段名匹配 */
    private String aliases;

    /** 字段分类，如 contact、money、audit */
    private String category;

    /** 关联代码集/枚举字典 ID */
    @TableField("code_set_id")
    private Long codeSetId;

    /** 是否敏感字段 */
    private Boolean sensitive;

    /** 字段状态: draft/enabled/deprecated/disabled */
    private String status;

    /** 生命周期替代字段 ID，仅允许指向同项目标准字段 */
    @TableField("replacement_field_id")
    private Long replacementFieldId;

    /** 生命周期替代说明、迁移建议或历史兼容原因 */
    @TableField("replacement_reason")
    private String replacementReason;

    /** 示例值 */
    @TableField("example_value")
    private String exampleValue;

    /** 字段值格式类型，如 money/mobile/email/timestamp/json/status */
    @TableField("format_type")
    private String formatType;

    /** 字段值格式正则或轻量模式说明 */
    @TableField("format_pattern")
    private String formatPattern;

    /** 字段值单位，如 cent/yuan/ms/UTC */
    @TableField("format_unit")
    private String formatUnit;

    /** 字段值精度说明，如 scale=2/millisecond/6dp */
    @TableField("format_precision")
    private String formatPrecision;

    /** 时间类字段时区说明 */
    @TableField("format_timezone")
    private String formatTimezone;

    /** 字段值空值策略说明，不改变 nullable 数据库约束 */
    @TableField("format_null_policy")
    private String formatNullPolicy;

    /** 字段值正例 JSON 字符串数组 */
    @TableField("valid_examples_json")
    private String validExamplesJson;

    /** 字段值反例 JSON 字符串数组 */
    @TableField("invalid_examples_json")
    private String invalidExamplesJson;

    /** 字段值格式补充说明 */
    @TableField("format_notes")
    private String formatNotes;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Boolean isDeleted;
}
