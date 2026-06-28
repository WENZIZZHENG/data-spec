package com.dataspec.prompt.model;

import jakarta.validation.constraints.NotBlank;

/**
 * Prompt 输出评测请求。
 */
public record PromptTemplateEvalReq(
        @NotBlank(message = "模板 key 不能为空") String templateKey,
        @NotBlank(message = "待评测输出不能为空") String output
) {
}
