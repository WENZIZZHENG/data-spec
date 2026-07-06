package com.dataspec.syntheticexample.model;

import java.util.List;
import java.util.Map;

/**
 * 合成标准样例包，是 API/CLI/AI 共享的稳定 JSON 契约。
 *
 * @param kind 契约类型，固定为 dataspec.synthetic-standard-examples。
 * @param schemaVersion 输出 schema 版本。
 * @param projectId DataSpec 项目 ID。
 * @param scenario 生成场景。
 * @param specHash 基于标准摘要和生成参数计算的稳定 hash。
 * @param generationParams 本次生成参数。
 * @param sourceSummary 项目标准素材与 fallback 使用摘要。
 * @param goodSql 推荐 SQL/DDL 样例。
 * @param badSql 反例 SQL/DDL 样例。
 * @param ddlPreviewInputs 可复用的 DDL preview 输入。
 * @param fieldSuggestionQuestions 字段推荐评测问题。
 * @param standardQaCases 标准问答案例。
 * @param expectedDiagnostics 反例预期诊断集合。
 * @param diagnostics 生成过程诊断，如 fallback 使用说明。
 * @param safety 只读与脱敏安全边界。
 * @param nextActions 建议的人工审核和后续验证动作。
 */
public record SyntheticStandardExamplePackage(
        String kind,
        int schemaVersion,
        Long projectId,
        String scenario,
        String specHash,
        Map<String, Object> generationParams,
        SyntheticExampleSourceSummary sourceSummary,
        List<SyntheticSqlCase> goodSql,
        List<SyntheticSqlCase> badSql,
        List<SyntheticDdlPreviewInput> ddlPreviewInputs,
        List<SyntheticFieldSuggestionQuestion> fieldSuggestionQuestions,
        List<SyntheticStandardQaCase> standardQaCases,
        List<SyntheticExampleDiagnostic> expectedDiagnostics,
        List<SyntheticExampleDiagnostic> diagnostics,
        SyntheticExampleSafety safety,
        List<String> nextActions
) {
}
