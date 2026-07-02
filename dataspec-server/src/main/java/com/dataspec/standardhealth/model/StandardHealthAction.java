package com.dataspec.standardhealth.model;

/**
 * 标准健康改进动作。
 */
public record StandardHealthAction(
        String title,
        String description,
        String priority,
        String targetRoute,
        String evidence
) {
}
