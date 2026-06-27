package com.dataspec.aireplay.model;

import com.dataspec.aireplay.entity.AiJobRecord;

import java.util.Map;

/**
 * AI 作业详情，包含解析后的输入输出和可复制回放信息。
 */
public record AiJobRecordDetail(
        AiJobRecord record,
        Object inputPayload,
        Object outputPayload,
        Map<String, Object> replayPayload,
        String replayCommand
) {
}
