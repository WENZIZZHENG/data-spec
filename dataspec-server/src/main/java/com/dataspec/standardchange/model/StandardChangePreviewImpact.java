package com.dataspec.standardchange.model;

import java.util.Map;

/**
 * 变更可能影响的下游链路。
 */
public record StandardChangePreviewImpact(
        String impactType,
        String severity,
        Long sourceId,
        String title,
        int count,
        String description,
        Map<String, Object> metadata
) {
}
