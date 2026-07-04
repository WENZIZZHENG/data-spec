package com.dataspec.aitaskrun.model;

/**
 * 已完成的任务产物摘要。只保存引用和摘要，不保存大 payload。
 */
public record AiTaskPartialArtifact(
        String type,
        String name,
        String ref,
        String summary
) {
}
