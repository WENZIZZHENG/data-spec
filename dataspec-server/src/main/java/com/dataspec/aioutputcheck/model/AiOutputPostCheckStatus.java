package com.dataspec.aioutputcheck.model;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * AI 输出后置校验状态。
 */
@Schema(description = "AI 输出后置校验状态；PASS 才表示 safeToUse=true，WARN/FAIL 默认需要人工或 AI 修复。")
public enum AiOutputPostCheckStatus {
    /** 高置信引用全部解析为当前兼容标准，且不存在阻断问题。 */
    PASS,
    /** 仅存在过期、歧义、低置信或证据缺口问题，默认不直接使用。 */
    WARN,
    /** 存在未知高置信引用、跨项目引用、未知规则或不兼容快照等阻断问题。 */
    FAIL
}
