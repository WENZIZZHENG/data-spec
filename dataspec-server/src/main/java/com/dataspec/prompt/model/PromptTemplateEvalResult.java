package com.dataspec.prompt.model;

import java.util.List;

/**
 * Prompt 模板评测结果。
 */
public record PromptTemplateEvalResult(
        String templateKey,
        String promptVersion,
        boolean passed,
        List<PromptTemplateEvalFailure> failures,
        List<String> requiredSections,
        List<String> requiredPhrases,
        String diff
) {
}
