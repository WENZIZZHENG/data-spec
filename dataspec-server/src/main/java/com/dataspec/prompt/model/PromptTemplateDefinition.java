package com.dataspec.prompt.model;

import java.util.List;

/**
 * AI 可消费的 Prompt/生成任务模板定义。
 *
 * <p>templateKey 和 promptVersion 是 AI 回放、golden 评测和后续兼容判断的公共契约。</p>
 */
public record PromptTemplateDefinition(
        String templateKey,
        String promptVersion,
        String scenario,
        String title,
        String outputFormat,
        List<String> requiredSections,
        List<String> requiredPhrases,
        List<String> changeLog
) {
}
