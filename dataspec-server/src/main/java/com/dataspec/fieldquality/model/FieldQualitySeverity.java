package com.dataspec.fieldquality.model;

/**
 * 字段质量问题严重级别，复用 SQL lint 的三段语义，方便前端和 AI 消费。
 */
public enum FieldQualitySeverity {
    ERROR,
    WARNING,
    SUGGESTION
}
