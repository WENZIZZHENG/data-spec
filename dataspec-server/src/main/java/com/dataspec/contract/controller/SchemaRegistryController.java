package com.dataspec.contract.controller;

import com.dataspec.common.result.R;
import com.dataspec.contract.model.SchemaContract;
import com.dataspec.contract.model.SchemaRegistryCatalog;
import com.dataspec.contract.service.SchemaRegistryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/contracts")
@RequiredArgsConstructor
public class SchemaRegistryController {

    private final SchemaRegistryService schemaRegistryService;

    @GetMapping
    public R<SchemaRegistryCatalog> listContracts() {
        return R.ok(schemaRegistryService.getCatalog());
    }

    @GetMapping("/{contractId}")
    public R<SchemaContract> getContract(@PathVariable String contractId) {
        return R.ok(schemaRegistryService.getContract(contractId));
    }
}
