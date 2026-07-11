package com.dataspec.fieldsemantic.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 字段语义规则持久化对象，描述字段派生、单位换算、聚合口径、时间粒度和 source of truth guidance。
 */
@Data
@TableName("ds_field_semantic_rule")
public class FieldSemanticRule {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属项目 ID，只能在项目授权范围内读写 */
    private Long projectId;

    /** 目标标准字段 ID，必须属于同一项目 */
    private Long fieldId;

    /** 可选源字段 ID，用于 derivedFrom 或 source-of-truth 关系，必须属于同一项目 */
    private Long sourceFieldId;

    /** 语义规则类型，如 DERIVED_FROM、UNIT_CONVERSION、AGGREGATION、TIME_GRAIN、SOURCE_OF_TRUTH、NAMING */
    private String ruleType;

    /** 单位换算说明，只做 guidance，不执行真实数据计算 */
    private String unitConversion;

    /** 聚合口径说明，如 sum/count/distinct/ratio，不替代指标平台 */
    private String aggregationRule;

    /** 时间粒度说明，如 timestamp、date、day、month */
    private String timeGranularity;

    /** source of truth 或首选字段说明，用于 AI 避免口径混用 */
    private String sourceOfTruth;

    /** 推荐使用场景；不得包含真实业务数据行或凭据 */
    private String recommendedUse;

    /** 常见误用、反例或禁用场景；不得包含 raw secret 或业务数据行 */
    private String antiPatterns;

    /** 证据引用数组 JSON，可关联标准示例、决策记录或文档片段 */
    @Schema(description = "证据引用数组 JSON，可关联标准示例、决策记录或文档片段。")
    private String evidenceRefsJson;

    /** 语义规则状态，enabled 表示默认进入知识卡和 AI Context */
    private String status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Boolean isDeleted;
}
