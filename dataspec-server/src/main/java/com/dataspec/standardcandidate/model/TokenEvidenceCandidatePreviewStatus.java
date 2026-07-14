package com.dataspec.standardcandidate.model;

import io.swagger.v3.oas.annotations.media.Schema;

/** 命名证据候选预览状态。 */
@Schema(description = "命名证据候选预览状态；只有 READY 可进入确认写入。")
public enum TokenEvidenceCandidatePreviewStatus {
    /** 存在可操作 signals 且没有字段或候选冲突。 */
    READY,
    /** 当前名称没有未知词、歧义缩写或禁用词。 */
    NO_ACTIONABLE_SIGNAL,
    /** 项目内已有同名标准字段。 */
    STANDARD_EXISTS,
    /** 完整去重键对应的 TOKEN_EVIDENCE 候选已存在。 */
    EXACT_DUPLICATE,
    /** 同名 active 候选来自其他来源事实。 */
    NAME_CONFLICT
}
