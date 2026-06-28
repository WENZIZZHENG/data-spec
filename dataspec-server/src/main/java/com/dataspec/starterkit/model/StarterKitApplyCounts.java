package com.dataspec.starterkit.model;

public record StarterKitApplyCounts(
        int domains,
        int enums,
        int enumValues,
        int fields,
        int templates,
        int templateFields
) {
    public static StarterKitApplyCounts empty() {
        return new StarterKitApplyCounts(0, 0, 0, 0, 0, 0);
    }

    public StarterKitApplyCounts plusDomains(int value) {
        return new StarterKitApplyCounts(domains + value, enums, enumValues, fields, templates, templateFields);
    }

    public StarterKitApplyCounts plusEnums(int value) {
        return new StarterKitApplyCounts(domains, enums + value, enumValues, fields, templates, templateFields);
    }

    public StarterKitApplyCounts plusEnumValues(int value) {
        return new StarterKitApplyCounts(domains, enums, enumValues + value, fields, templates, templateFields);
    }

    public StarterKitApplyCounts plusFields(int value) {
        return new StarterKitApplyCounts(domains, enums, enumValues, fields + value, templates, templateFields);
    }

    public StarterKitApplyCounts plusTemplates(int value) {
        return new StarterKitApplyCounts(domains, enums, enumValues, fields, templates + value, templateFields);
    }

    public StarterKitApplyCounts plusTemplateFields(int value) {
        return new StarterKitApplyCounts(domains, enums, enumValues, fields, templates, templateFields + value);
    }
}
