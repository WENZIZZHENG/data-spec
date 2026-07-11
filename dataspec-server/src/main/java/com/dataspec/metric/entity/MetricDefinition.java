package com.dataspec.metric.entity;

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
 * 轻量指标口径定义，描述业务指标如何映射到标准字段、过滤条件、聚合方式和时间粒度。
 */
@Data
@TableName("ds_metric_definition")
public class MetricDefinition {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属项目 ID，只能在项目授权范围内读写 */
    private Long projectId;

    /** 项目内唯一指标键，建议使用 snake_case，不含凭据或业务数据行 */
    private String metricKey;

    /** 指标展示名称，如订单金额、支付成功率 */
    private String displayName;

    /** 指标业务定义文本，说明统计边界和含义 */
    private String definition;

    /** 度量字段引用数组 JSON，字段必须属于同一项目 */
    @Schema(description = "度量字段引用数组 JSON，字段必须属于同一项目。")
    private String measureFieldsJson;

    /** 维度字段引用数组 JSON，字段必须属于同一项目 */
    @Schema(description = "维度字段引用数组 JSON，字段必须属于同一项目。")
    private String dimensionFieldsJson;

    /** 指标过滤口径说明，如状态、时间范围或软删除条件；不自动改写 SQL */
    private String filterRule;

    /** 指标聚合口径说明，如 sum/count/distinct/ratio */
    private String aggregationRule;

    /** 指标默认时间粒度，如 day、week、month */
    private String timeGrain;

    /** 维护者说明或取舍记录；不得包含凭据或业务数据行 */
    private String ownerNotes;

    /** 示例 SQL，仅作说明和 AI guidance，不会被执行 */
    private String exampleSql;

    /** 证据引用数组 JSON，可关联字段、示例、决策记录或文档片段 */
    @Schema(description = "证据引用数组 JSON，可关联字段、示例、决策记录或文档片段。")
    private String evidenceRefsJson;

    /** 指标口径状态，enabled 表示默认进入知识卡和 AI Context */
    private String status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Boolean isDeleted;
}
