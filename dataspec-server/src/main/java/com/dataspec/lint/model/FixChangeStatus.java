package com.dataspec.lint.model;

/**
 * 单条 fixedSql 修复建议在当前策略下的状态。
 */
public enum FixChangeStatus {
    /** 已用于生成 fixedSql */
    APPLIED,

    /** dry-run 预览中计划使用，尚未建议直接应用 */
    PLANNED,

    /** 已识别但未用于生成 fixedSql */
    SKIPPED
}
