package com.dataspec.querynormalization.model;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 查询 token 的项目词典解析状态。
 */
@Schema(description = "查询 token 的词典解析状态；歧义、禁用和未解析状态均不得绑定高置信 canonical 字段。")
public enum QueryTokenResolutionStatus {
    /** 已由当前项目启用 glossary 唯一解析。 */
    RESOLVED,
    /** 同一 token 指向多个 canonical 字段，需要人工确认。 */
    AMBIGUOUS,
    /** 命中当前项目 glossary 的禁用词。 */
    DISABLED,
    /** 没有当前项目 glossary 证据，不做猜测。 */
    UNRESOLVED
}
