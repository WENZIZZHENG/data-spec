package com.dataspec.standardcandidate.model;

import io.swagger.v3.oas.annotations.media.Schema;

/** 命名证据候选的可操作信号类型。 */
@Schema(description = "可触发候选预览的命名证据信号；所有类型都必须人工确认。")
public enum TokenEvidenceCandidateSignalType {
    /** 当前项目 glossary 未解析的业务词。 */
    UNKNOWN_TERM,
    /** 同一缩写指向多个 canonical 字段。 */
    AMBIGUOUS_ABBREVIATION,
    /** 命中当前项目禁用词或禁用命名。 */
    DISABLED_NAMING
}
