package com.dataspec.testdata.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Map;

/**
 * 结构化 mock payload。
 *
 * @param payloadId 确定性 payload ID。
 * @param objectScenario 轻量对象场景。
 * @param payload 合成 mock 对象，字段值均来自 valid case。
 * @param sourceCaseIds 来源 valid case ID。
 * @param requiresBusinessReview 是否需要人工复核。
 */
@Schema(description = "结构化 mock payload，字段值均来自安全合成 valid case。")
public record TestDataMockPayload(
        @Schema(description = "确定性 payload ID。")
        String payloadId,
        @Schema(description = "轻量对象场景。")
        String objectScenario,
        @Schema(description = "合成 mock 对象，字段值均来自 valid case。")
        Map<String, Object> payload,
        @Schema(description = "来源 valid case ID。")
        List<String> sourceCaseIds,
        @Schema(description = "是否需要人工复核。")
        boolean requiresBusinessReview
) {
}
