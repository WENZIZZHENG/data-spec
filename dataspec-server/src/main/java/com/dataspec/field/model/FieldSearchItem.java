package com.dataspec.field.model;

import com.dataspec.explaintrace.model.ExplainTrace;
import com.dataspec.field.entity.Field;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 单个字段标准检索命中项。
 */
public record FieldSearchItem(
        Field field,
        int score,
        List<String> matchReasons,
        String recommendedUse,
        @Schema(description = "字段使用契约摘要，包含推荐使用、禁用场景、Join、默认过滤、聚合、替代指导和误用样例。")
        List<String> usageContractSummary,
        List<String> nextActions,
        List<ExplainTrace> evidence,
        @Schema(description = "项目内稳定字段引用，格式为 field:<projectId>:<fieldId>；供 AI/CLI/MCP 在字段改名后继续定位同一标准字段。")
        String stableRef,
        @Schema(description = "当前推荐 canonical 字段引用；废弃或停用字段存在有效 replacementFieldId 时指向替代字段 stableRef。")
        String canonicalRef,
        @Schema(description = "字段生命周期状态，如 enabled、draft、deprecated 或 disabled。")
        String lifecycleStatus,
        @Schema(description = "本次搜索命中的别名或历史名；非别名命中时为空。")
        String matchedAlias
) {
    public FieldSearchItem(Field field, int score, List<String> matchReasons, String recommendedUse, List<String> nextActions) {
        this(field, score, matchReasons, recommendedUse, List.of(), nextActions, List.of(), null, null, null, null);
    }

    public FieldSearchItem(
            Field field,
            int score,
            List<String> matchReasons,
            String recommendedUse,
            List<String> usageContractSummary,
            List<String> nextActions
    ) {
        this(field, score, matchReasons, recommendedUse, usageContractSummary, nextActions, List.of(), null, null, null, null);
    }

    public FieldSearchItem(
            Field field,
            int score,
            List<String> matchReasons,
            String recommendedUse,
            List<String> usageContractSummary,
            List<String> nextActions,
            List<ExplainTrace> evidence
    ) {
        this(field, score, matchReasons, recommendedUse, usageContractSummary, nextActions, evidence, null, null, null, null);
    }
}
