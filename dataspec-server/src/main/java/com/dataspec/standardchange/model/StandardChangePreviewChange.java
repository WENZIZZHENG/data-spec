package com.dataspec.standardchange.model;

/**
 * 单个属性的预期变更。
 */
public record StandardChangePreviewChange(
        String attribute,
        Object beforeValue,
        Object afterValue,
        String riskLevel,
        String description
) {
}
