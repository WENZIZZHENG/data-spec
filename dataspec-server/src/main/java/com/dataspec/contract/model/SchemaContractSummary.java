package com.dataspec.contract.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SchemaContractSummary {

    private String contractId;
    private String displayName;
    private String description;
    private String schemaVersion;
    private String jsonSchemaRef;
    private List<String> stableFields;
    private List<DeprecatedContractField> deprecatedFields;
    private SchemaCompatibilityPolicy compatibility;
    private String docsRef;
}
