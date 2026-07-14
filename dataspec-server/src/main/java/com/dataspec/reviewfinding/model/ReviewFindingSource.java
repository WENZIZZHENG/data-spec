package com.dataspec.reviewfinding.model;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Review Finding 的原始发现来源。
 */
@Schema(description = "Finding 的原始发现来源；Evidence Package 和 PR 评论沿用原始来源，不改写该值。")
public enum ReviewFindingSource {
    /** DataSpec 确定性 SQL lint 规则。 */
    SQL_LINT,
    /** DataSpec 确定性 AI output post-check。 */
    AI_OUTPUT_POSTCHECK,
    /** 由外部 AI 提交、等待 DataSpec post-check 的结构化 finding。 */
    EXTERNAL_AI
}
