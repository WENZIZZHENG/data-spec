package com.dataspec.field.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
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

    /** 推荐使用场景，说明字段适合用于哪些 SQL、指标、写入或 DDL 场景；不得包含凭据或业务数据行 */
    @Schema(description = "字段推荐使用场景，说明字段适合用于哪些 SQL、指标、写入或 DDL 场景；不得包含凭据或业务数据行。")
    @TableField(value = "preferred_use_cases", updateStrategy = FieldStrategy.ALWAYS)
    private String preferredUseCases;

    /** 禁用或需确认场景，AI 命中这些场景时不得直接采纳；不得包含密码、token、完整 JDBC URL、DSN 或私钥 */
    @Schema(description = "字段禁用或需确认场景，AI 命中这些场景时不得直接采纳；不得包含密码、token、完整 JDBC URL、DSN 或私钥。")
    @TableField(value = "avoid_when", updateStrategy = FieldStrategy.ALWAYS)
    private String avoidWhen;

    /** Join 使用提示，如推荐关联键、关联方向或不适合 Join 的边界；只做只读指导 */
    @Schema(description = "字段 Join 使用提示，如推荐关联键、关联方向或不适合 Join 的边界；只做只读指导。")
    @TableField(value = "join_hints", updateStrategy = FieldStrategy.ALWAYS)
    private String joinHints;

    /** 默认过滤条件或统计口径提示，如状态、时间范围或软删除条件；不自动改写 SQL */
    @Schema(description = "字段默认过滤条件或统计口径提示，如状态、时间范围或软删除条件；不自动改写 SQL。")
    @TableField(value = "default_filters", updateStrategy = FieldStrategy.ALWAYS)
    private String defaultFilters;

    /** 聚合口径提示，如 sum/count/distinct/单位换算；不替代指标平台 */
    @Schema(description = "字段聚合口径提示，如 sum/count/distinct/单位换算；不替代指标平台。")
    @TableField(value = "aggregation_hints", updateStrategy = FieldStrategy.ALWAYS)
    private String aggregationHints;

    /** 特定场景下的替代字段或迁移指导，与生命周期替代说明互补 */
    @Schema(description = "字段在特定场景下的替代字段或迁移指导，与生命周期替代说明互补。")
    @TableField(value = "replacement_guidance", updateStrategy = FieldStrategy.ALWAYS)
    private String replacementGuidance;

    /** 常见误用或反例说明，用于 AI 低置信提示；不得包含真实业务数据行或凭据 */
    @Schema(description = "字段常见误用或反例说明，用于 AI 低置信提示；不得包含真实业务数据行或凭据。")
    @TableField(value = "misuse_examples", updateStrategy = FieldStrategy.ALWAYS)
    private String misuseExamples;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Boolean isDeleted;
}
