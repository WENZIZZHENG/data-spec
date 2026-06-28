package com.dataspec.starterkit.model;

public record StarterKitFieldDefinition(
        String name,
        String displayName,
        String dataType,
        Integer length,
        Integer precisionVal,
        Integer scaleVal,
        Boolean nullable,
        String defaultValue,
        String comment,
        String domainCode,
        String tags,
        String aliases,
        String category,
        String codeSetCode,
        Boolean sensitive,
        String status,
        String exampleValue
) {
}
