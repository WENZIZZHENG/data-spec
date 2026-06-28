package com.dataspec.starterkit.model;

public record StarterKitTemplateField(
        String fieldName,
        String name,
        String dataType,
        Boolean nullable,
        String defaultValue,
        String comment,
        Integer sortOrder,
        Boolean required
) {
}
