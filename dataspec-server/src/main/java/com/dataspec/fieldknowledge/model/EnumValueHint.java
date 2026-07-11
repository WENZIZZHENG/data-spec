package com.dataspec.fieldknowledge.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;

/**
 * 字段知识卡中的枚举值生命周期提示。
 *
 * @param value 枚举值
 * @param label 显示标签
 * @param status 生命周期状态
 * @param aliases 别名列表
 * @param replacementValue 替代枚举值
 * @param validFrom 有效期开始日期
 * @param validTo 有效期结束日期
 * @param mappingHints 跨系统映射提示
 * @param aiUsageNotes AI 使用说明
 */
@Schema(description = "字段知识卡中的枚举值生命周期提示。")
public record EnumValueHint(
        String value,
        String label,
        String status,
        List<String> aliases,
        String replacementValue,
        LocalDate validFrom,
        LocalDate validTo,
        String mappingHints,
        String aiUsageNotes
) {
}
