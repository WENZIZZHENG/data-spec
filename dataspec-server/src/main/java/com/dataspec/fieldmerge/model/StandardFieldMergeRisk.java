package com.dataspec.fieldmerge.model;

/**
 * 标准字段合并风险项。
 *
 * @param severity     风险级别，常用值为 ERROR、WARNING、INFO。
 * @param code         稳定风险码，供前端和 AI 判断是否阻断。
 * @param message      脱敏后的风险说明。
 * @param blocking     true 表示 apply 必须阻断。
 * @param manualAction 用户可执行的人工处理建议。
 */
public record StandardFieldMergeRisk(
        String severity,
        String code,
        String message,
        boolean blocking,
        String manualAction
) {
}
