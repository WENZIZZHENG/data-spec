package com.dataspec.standard.dto;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * 已保存标准快照的只读 payload，供历史导出和回放使用。
 */
public record StandardSnapshotPayload(
        StandardSnapshotInfo standard,
        JsonNode payload,
        int fieldCount,
        int enumCount,
        int ruleCount
) {
}
