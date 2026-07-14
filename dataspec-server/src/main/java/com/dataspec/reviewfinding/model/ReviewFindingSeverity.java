package com.dataspec.reviewfinding.model;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 跨 lint、post-check 和评审交付共享的问题级别。
 */
@Schema(description = "共享问题级别；ERROR 可阻断使用，WARNING 需确认，SUGGESTION/INFO 为非阻断建议。")
public enum ReviewFindingSeverity {
    /** 必须修复后才能采纳结果。 */
    ERROR,
    /** 需要人工确认或补充证据。 */
    WARNING,
    /** 不阻断结果的改进建议。 */
    SUGGESTION,
    /** 仅用于提供上下文。 */
    INFO
}
