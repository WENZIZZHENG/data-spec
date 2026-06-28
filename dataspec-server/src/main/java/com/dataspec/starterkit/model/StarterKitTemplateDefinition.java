package com.dataspec.starterkit.model;

import java.util.List;

public record StarterKitTemplateDefinition(
        String name,
        String description,
        String tablePrefix,
        List<StarterKitTemplateField> fields
) {
}
