package com.dataspec.fieldknowledge.model;

import com.dataspec.fieldsemantic.model.FieldSemanticRuleResp;
import com.dataspec.metric.model.MetricDefinitionResp;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

/**
 * AI 可读字段知识卡，聚合字段标准、格式、usage contract、语义规则、枚举生命周期、指标引用和命名证据。
 *
 * @param projectId 所属项目 ID
 * @param fieldId 字段 ID
 * @param stableRef 项目内稳定字段引用
 * @param name 标准字段名
 * @param displayName 字段展示名称
 * @param dataType 数据类型
 * @param lifecycleStatus 字段生命周期状态
 * @param aliases 字段别名列表
 * @param formatSummary 字段格式约束摘要
 * @param usageContractSummary 字段 usage contract 摘要
 * @param namingGuidance 命名翻译和禁用翻译提示
 * @param semanticRules 字段语义规则列表
 * @param enumHints 枚举生命周期提示
 * @param usageExamples 字段使用正例、反例或维护理由摘要
 * @param metricReferences 指标口径引用
 * @param relatedFieldRefs 来源、替代或派生相关字段的稳定引用
 * @param riskNotes 风险、边界或需人工确认提示
 * @param evidenceRefs 语义规则、指标口径和示例来源的证据引用
 * @param lastVerifiedAt 知识卡来源最近更新时间
 */
@Schema(description = "AI 可读字段知识卡。")
public record FieldKnowledgeCardResp(
        Long projectId,
        Long fieldId,
        String stableRef,
        String name,
        String displayName,
        String dataType,
        String lifecycleStatus,
        List<String> aliases,
        List<String> formatSummary,
        List<String> usageContractSummary,
        List<String> namingGuidance,
        List<FieldSemanticRuleResp> semanticRules,
        List<EnumValueHint> enumHints,
        List<String> usageExamples,
        List<MetricDefinitionResp> metricReferences,
        List<String> relatedFieldRefs,
        List<String> riskNotes,
        List<String> evidenceRefs,
        LocalDateTime lastVerifiedAt
) {
}
