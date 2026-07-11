package com.dataspec.enumdict.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
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

    /** 枚举值生命周期状态：enabled、deprecated、disabled 或 draft */
    @Schema(description = "枚举值生命周期状态：enabled、deprecated、disabled 或 draft。")
    @TableField(value = "status", updateStrategy = FieldStrategy.ALWAYS)
    private String status;

    /** 枚举值别名数组 JSON，用于 AI 识别历史值、展示值或外部系统映射 */
    @Schema(description = "枚举值别名数组 JSON，用于 AI 识别历史值、展示值或外部系统映射。")
    @TableField(value = "aliases_json", updateStrategy = FieldStrategy.ALWAYS)
    private String aliasesJson;

    /** 废弃或停用枚举值的推荐替代值，仅作 guidance，不自动改写 SQL */
    @Schema(description = "废弃或停用枚举值的推荐替代值，仅作 guidance，不自动改写 SQL。")
    @TableField(value = "replacement_value", updateStrategy = FieldStrategy.ALWAYS)
    private String replacementValue;

    /** 枚举值有效期开始日期，可为空 */
    @Schema(description = "枚举值有效期开始日期，可为空。")
    @TableField(value = "valid_from", updateStrategy = FieldStrategy.ALWAYS)
    private LocalDate validFrom;

    /** 枚举值有效期结束日期，可为空 */
    @Schema(description = "枚举值有效期结束日期，可为空。")
    @TableField(value = "valid_to", updateStrategy = FieldStrategy.ALWAYS)
    private LocalDate validTo;

    /** 枚举值来源证据或维护说明；不得包含凭据或业务数据行 */
    @Schema(description = "枚举值来源证据或维护说明；不得包含凭据或业务数据行。")
    @TableField(value = "source_evidence", updateStrategy = FieldStrategy.ALWAYS)
    private String sourceEvidence;

    /** 枚举值跨系统映射提示，如外部编码、展示名或兼容说明 */
    @Schema(description = "枚举值跨系统映射提示，如外部编码、展示名或兼容说明。")
    @TableField(value = "mapping_hints", updateStrategy = FieldStrategy.ALWAYS)
    private String mappingHints;

    /** 枚举值 AI 使用说明；不得包含 token、密码、完整 JDBC URL、DSN 或业务数据行 */
    @Schema(description = "枚举值 AI 使用说明；不得包含 token、密码、完整 JDBC URL、DSN 或业务数据行。")
    @TableField(value = "ai_usage_notes", updateStrategy = FieldStrategy.ALWAYS)
    private String aiUsageNotes;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Boolean isDeleted;
}
