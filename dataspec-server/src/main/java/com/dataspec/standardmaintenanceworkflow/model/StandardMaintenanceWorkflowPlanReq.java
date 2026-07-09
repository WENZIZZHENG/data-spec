package com.dataspec.standardmaintenanceworkflow.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 标准维护 workflow dry-run 计划请求。
 */
@Data
@Schema(description = "标准维护 workflow dry-run 计划请求；只携带来源筛选和页面上下文，不触发写入。")
public class StandardMaintenanceWorkflowPlanReq {

    /** 计划所属 DataSpec 项目 ID。 */
    @NotNull(message = "项目ID不能为空")
    @Schema(description = "计划所属 DataSpec 项目 ID。", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long projectId;

    /** 维护来源类型，例如 STANDARD_CANDIDATE、FIELD_QUALITY、FIELD_COVERAGE 或 AI_TASK_FAILURE。 */
    @Schema(description = "维护来源类型，例如 STANDARD_CANDIDATE、FIELD_QUALITY、FIELD_COVERAGE 或 AI_TASK_FAILURE。")
    private String sourceType;

    /** 来源对象 ID 列表，例如候选 ID、字段 ID 或任务运行 ID；为空时按当前来源摘要生成计划。 */
    @Schema(description = "来源对象 ID 列表，例如候选 ID、字段 ID 或任务运行 ID；为空时按当前来源摘要生成计划。")
    private List<Long> sourceIds = new ArrayList<>();

    /** 字段质量问题代码筛选；仅用于生成计划摘要，不直接修复字段。 */
    @Schema(description = "字段质量问题代码筛选；仅用于生成计划摘要，不直接修复字段。")
    private List<String> issueCodes = new ArrayList<>();

    /** 覆盖率状态筛选，例如 UNMANAGED、POSSIBLE_DUPLICATE、MISSING_COMMENT。 */
    @Schema(description = "覆盖率状态筛选，例如 UNMANAGED、POSSIBLE_DUPLICATE、MISSING_COMMENT。")
    private List<String> coverageStatuses = new ArrayList<>();

    /** 来源报告完整性状态，例如 COMPLETE、PARTIAL、CANCELLED 或 FAILED。 */
    @Schema(description = "来源报告完整性状态，例如 COMPLETE、PARTIAL、CANCELLED 或 FAILED。")
    private String sourceStatus;

    /** 覆盖率来源中未纳入统计的失败表数量；只用于 evidence 摘要，不触发扫描或写入。 */
    @Schema(description = "覆盖率来源中未纳入统计的失败表数量；只用于 evidence 摘要，不触发扫描或写入。")
    private Integer failedTableCount;

    /** 覆盖率来源中跳过或未扫描表数量；只用于保留 partial 边界，不触发扫描或写入。 */
    @Schema(description = "覆盖率来源中跳过或未扫描表数量；只用于保留 partial 边界，不触发扫描或写入。")
    private Integer skippedTableCount;

    /** 页面或调用方已知的待处理项数量；没有持久报告时用于生成证据摘要。 */
    @Schema(description = "页面或调用方已知的待处理项数量；没有持久报告时用于生成证据摘要。")
    private Integer itemCount;

    /** 可返回给用户的来源页面 route；服务端会脱敏后再放入 evidence。 */
    @Schema(description = "可返回给用户的来源页面 route；服务端会脱敏后再放入 evidence。")
    private String sourceRoute;

    /** 调用方补充的非敏感说明；服务端会脱敏并截断后用于计划描述。 */
    @Schema(description = "调用方补充的非敏感说明；服务端会脱敏并截断后用于计划描述。")
    private String note;
}
