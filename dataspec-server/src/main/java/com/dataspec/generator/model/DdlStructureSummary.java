package com.dataspec.generator.model;

import java.util.List;

/**
 * DDL preview 消费表结构标准后的只读摘要。
 *
 * @param appliedConstraints 已拼入 CREATE TABLE 的结构化约束说明
 * @param generatedIndexes 已生成的 CREATE INDEX 语句说明
 * @param skippedHints 因 advisory、字段缺失或不安全而跳过的提示
 * @param policyNotes 审计、软删除、CHECK 和方言说明等只读 guidance
 * @param evidence 结构标准来源证据摘要
 */
public record DdlStructureSummary(
        List<String> appliedConstraints,
        List<String> generatedIndexes,
        List<String> skippedHints,
        List<String> policyNotes,
        List<String> evidence
) {
    public static DdlStructureSummary empty() {
        return new DdlStructureSummary(List.of(), List.of(), List.of(), List.of(), List.of());
    }
}
