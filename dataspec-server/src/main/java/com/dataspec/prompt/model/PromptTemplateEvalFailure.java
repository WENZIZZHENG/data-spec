package com.dataspec.prompt.model;

/**
 * Prompt 模板评测失败项。
 */
public record PromptTemplateEvalFailure(
        String kind,
        String marker,
        String message
) {
}
