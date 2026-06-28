package com.dataspec.starterkit.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record StarterKitDefinition(
        String key,
        String name,
        String version,
        String description,
        List<String> tags,
        List<String> useCases,
        List<StarterKitDomain> domains,
        List<StarterKitEnumDefinition> enums,
        List<StarterKitFieldDefinition> fields,
        List<StarterKitTemplateDefinition> templates
) {
    @JsonProperty("fieldCount")
    public int fieldCount() {
        return fields == null ? 0 : fields.size();
    }

    @JsonProperty("enumCount")
    public int enumCount() {
        return enums == null ? 0 : enums.size();
    }

    @JsonProperty("templateCount")
    public int templateCount() {
        return templates == null ? 0 : templates.size();
    }
}
