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
public class SchemaRegistryCatalog {

    private String kind;
    private Integer schemaVersion;
    private String registryVersion;
    private SchemaCompatibilityPolicy compatibilityPolicy;
    private List<SchemaContractSummary> contracts;
    private List<String> requiredContractIds;
    private List<String> nextActions;
}
