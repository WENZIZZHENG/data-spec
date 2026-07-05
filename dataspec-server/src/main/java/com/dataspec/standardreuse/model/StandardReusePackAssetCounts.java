package com.dataspec.standardreuse.model;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 标准复用包资产数量摘要。
 */
@Schema(description = "标准复用包内字段、枚举、规则和模板的数量摘要。")
public record StandardReusePackAssetCounts(
        @Schema(description = "数据域数量。") Integer domains,
        @Schema(description = "标准字段数量。") Integer fields,
        @Schema(description = "枚举字典数量。") Integer enums,
        @Schema(description = "枚举值数量。") Integer enumValues,
        @Schema(description = "规则配置数量。") Integer rules,
        @Schema(description = "表模板数量。") Integer templates,
        @Schema(description = "模板字段数量。") Integer templateFields
) {
    public static StandardReusePackAssetCounts empty() {
        return new StandardReusePackAssetCounts(0, 0, 0, 0, 0, 0, 0);
    }

    public StandardReusePackAssetCounts plusDomains(int value) {
        return new StandardReusePackAssetCounts(domains + value, fields, enums, enumValues, rules, templates, templateFields);
    }

    public StandardReusePackAssetCounts plusFields(int value) {
        return new StandardReusePackAssetCounts(domains, fields + value, enums, enumValues, rules, templates, templateFields);
    }

    public StandardReusePackAssetCounts plusEnums(int value) {
        return new StandardReusePackAssetCounts(domains, fields, enums + value, enumValues, rules, templates, templateFields);
    }

    public StandardReusePackAssetCounts plusEnumValues(int value) {
        return new StandardReusePackAssetCounts(domains, fields, enums, enumValues + value, rules, templates, templateFields);
    }

    public StandardReusePackAssetCounts plusRules(int value) {
        return new StandardReusePackAssetCounts(domains, fields, enums, enumValues, rules + value, templates, templateFields);
    }

    public StandardReusePackAssetCounts plusTemplates(int value) {
        return new StandardReusePackAssetCounts(domains, fields, enums, enumValues, rules, templates + value, templateFields);
    }

    public StandardReusePackAssetCounts plusTemplateFields(int value) {
        return new StandardReusePackAssetCounts(domains, fields, enums, enumValues, rules, templates, templateFields + value);
    }
}
