package com.dataspec.aibatch.model;

/**
 * 交付包证据项，用于 AI 说明本次结果依据。
 */
public record AiBatchEvidence(
        String kind,
        String name,
        String value
) {
}
