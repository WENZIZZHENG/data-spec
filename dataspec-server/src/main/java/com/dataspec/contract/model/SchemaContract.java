package com.dataspec.contract.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SchemaContract {

    private String contractId;
    private String displayName;
    private String description;
    private String schemaVersion;
    private String jsonSchemaRef;
    private Map<String, Object> jsonSchema;
    private List<String> stableFields;
    private List<DeprecatedContractField> deprecatedFields;
    private SchemaCompatibilityPolicy compatibility;
    private String docsRef;
    private List<Map<String, Object>> examples;
}
