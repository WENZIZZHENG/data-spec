package com.dataspec.contract.service;

import com.dataspec.contract.model.SchemaContract;
import com.dataspec.contract.model.SchemaRegistryCatalog;

import java.util.List;
import java.util.Map;

public interface SchemaRegistryService {

    String REGISTRY_FILE = ".dataspec/schema-registry.json";

    SchemaRegistryCatalog getCatalog();

    SchemaContract getContract(String contractId);

    List<String> requiredContractIds();

    Map<String, Object> manifestSummary();
}
