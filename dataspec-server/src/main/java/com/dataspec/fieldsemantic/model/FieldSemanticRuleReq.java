package com.dataspec.fieldsemantic.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * 字段语义规则保存请求。
 *
 * @param projectId 所属项目 ID，创建时必填；更新时若提供则必须与原记录一致
 * @param fieldId 目标标准字段 ID，必须属于同一项目
 * @param sourceFieldId 可选源字段 ID，必须属于同一项目
 * @param ruleType 语义规则类型，如 DERIVED_FROM、UNIT_CONVERSION、AGGREGATION、TIME_GRAIN、SOURCE_OF_TRUTH、NAMING
 * @param unitConversion 单位换算说明，只做 guidance，不执行真实数据计算
 * @param aggregationRule 聚合口径说明，如 sum/count/distinct/ratio
 * @param timeGranularity 时间粒度说明，如 timestamp、date、day、month
 * @param sourceOfTruth source of truth 或首选字段说明
 * @param recommendedUse 推荐使用场景，不得包含真实业务数据行或凭据
 * @param antiPatterns 常见误用、反例或禁用场景，不得包含 raw secret 或业务数据行
 * @param evidenceRefs 证据引用列表，可关联标准示例、决策记录或文档片段
 * @param status 规则状态，enabled 表示默认进入知识卡和 AI Context
 */
@Schema(description = "字段语义规则保存请求。")
public record FieldSemanticRuleReq(
        @NotNull(message = "项目ID不能为空")
        @Schema(description = "所属项目 ID，创建时必填；更新时若提供则必须与原记录一致。")
        Long projectId,
        @NotNull(message = "字段ID不能为空")
        @Schema(description = "目标标准字段 ID，必须属于同一项目。")
        Long fieldId,
        @Schema(description = "可选源字段 ID，必须属于同一项目。")
        Long sourceFieldId,
        @NotBlank(message = "规则类型不能为空")
        @Schema(description = "语义规则类型，如 DERIVED_FROM、UNIT_CONVERSION、AGGREGATION、TIME_GRAIN、SOURCE_OF_TRUTH、NAMING。")
        String ruleType,
        @Schema(description = "单位换算说明，只做 guidance，不执行真实数据计算。")
        String unitConversion,
        @Schema(description = "聚合口径说明，如 sum/count/distinct/ratio。")
        String aggregationRule,
        @Schema(description = "时间粒度说明，如 timestamp、date、day、month。")
        String timeGranularity,
        @Schema(description = "source of truth 或首选字段说明。")
        String sourceOfTruth,
        @Schema(description = "推荐使用场景，不得包含真实业务数据行或凭据。")
        String recommendedUse,
        @Schema(description = "常见误用、反例或禁用场景，不得包含 raw secret 或业务数据行。")
        String antiPatterns,
        @Schema(description = "证据引用列表，可关联标准示例、决策记录或文档片段。")
        List<String> evidenceRefs,
        @Schema(description = "规则状态，enabled 表示默认进入知识卡和 AI Context。")
        String status
) {
}
