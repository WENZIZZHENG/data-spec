package com.dataspec.aifeedback.model;

/**
 * 报告级下一步动作。只提供导航和人工处理建议，不直接写入标准。
 */
public record AiFeedbackAction(
        String title,
        String description,
        String priority,
        String targetRoute
) {
}
