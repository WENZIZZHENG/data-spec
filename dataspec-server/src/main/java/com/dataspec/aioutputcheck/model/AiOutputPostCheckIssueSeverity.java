package com.dataspec.aioutputcheck.model;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * AI 输出后置校验问题级别。
 */
@Schema(description = "AI 输出后置校验问题级别；FAIL 会使整体 status=FAIL，WARN 会使整体 status 至少为 WARN。")
public enum AiOutputPostCheckIssueSeverity {
    /** 需要修复后才能采纳 AI 产物。 */
    FAIL,
    /** 需要人工确认或补证据后再使用。 */
    WARN
}
