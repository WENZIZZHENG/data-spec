package com.dataspec.starterkit.model;

import java.util.List;

public record StarterKitEnumDefinition(
        String code,
        String name,
        String description,
        String valueType,
        List<StarterKitEnumValue> values
) {
}
