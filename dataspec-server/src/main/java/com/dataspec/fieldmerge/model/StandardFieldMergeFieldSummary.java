package com.dataspec.fieldmerge.model;

import java.util.List;

/**
 * 标准字段合并预览中的字段摘要。
 *
 * @param id                字段 ID。
 * @param name              字段标准名。
 * @param displayName       字段显示名。
 * @param dataType          字段数据类型。
 * @param nullable          是否允许空值；冲突时只提示风险，不自动覆盖目标字段。
 * @param codeSetId         关联代码集 ID。
 * @param sensitive         是否敏感字段。
 * @param status            字段生命周期状态。
 * @param replacementFieldId 替代字段 ID，来源字段合并后会指向目标字段。
 * @param replacementReason 替代说明，响应中会做技术 secret 脱敏。
 * @param aliases           字段别名列表，预览中已去重。
 * @param tags              字段标签列表，预览中已去重。
 * @param exampleValue      字段示例值，响应中会做技术 secret 脱敏。
 * @param formatNotes       格式约束摘要，不包含源库行数据。
 * @param sourceSummaries   来源摘要，仅包含来源表列名称，不包含凭据、JDBC URL 或业务行值。
 */
public record StandardFieldMergeFieldSummary(
        Long id,
        String name,
        String displayName,
        String dataType,
        Boolean nullable,
        Long codeSetId,
        Boolean sensitive,
        String status,
        Long replacementFieldId,
        String replacementReason,
        List<String> aliases,
        List<String> tags,
        String exampleValue,
        List<String> formatNotes,
        List<String> sourceSummaries
) {
}
