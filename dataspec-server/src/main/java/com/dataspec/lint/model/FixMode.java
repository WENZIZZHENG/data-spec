package com.dataspec.lint.model;

/**
 * fixedSql 生成模式。
 */
public enum FixMode {
    /** 生成 fixedSql 候选并作为本次 lint 结果保存 */
    GENERATE,

    /** 生成预览 SQL 与变更计划，但明确标记为 dry-run */
    DRY_RUN,

    /** 不生成 fixedSql，仅返回可修复项解释 */
    DISABLED
}
