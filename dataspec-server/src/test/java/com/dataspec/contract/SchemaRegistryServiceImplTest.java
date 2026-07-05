package com.dataspec.contract;

import com.dataspec.common.exception.BizException;
import com.dataspec.contract.model.SchemaContract;
import com.dataspec.contract.model.SchemaRegistryCatalog;
import com.dataspec.contract.service.SchemaRegistryService;
import com.dataspec.contract.service.impl.SchemaRegistryServiceImpl;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SchemaRegistryServiceImplTest {

    private final SchemaRegistryService service = new SchemaRegistryServiceImpl();

    @Test
    void catalogExposesCoreAiContracts() {
        SchemaRegistryCatalog catalog = service.getCatalog();

        assertEquals("dataspec-schema-registry", catalog.getKind());
        assertEquals(1, catalog.getSchemaVersion());
        assertEquals("2026.07.05", catalog.getRegistryVersion());
        assertNotNull(catalog.getCompatibilityPolicy());
        assertTrue(catalog.getCompatibilityPolicy().getBreakingChangePolicy().contains("schemaVersion"));

        List<String> ids = catalog.getContracts().stream()
                .map(item -> item.getContractId())
                .toList();
        assertEquals(service.requiredContractIds(), ids);
        assertTrue(ids.containsAll(List.of(
                "field",
                "enum-dict",
                "rule-config",
                "standard-field-merge",
                "template",
                "standard-snapshot",
                "lint-result",
                "ai-evidence-package",
                "ai-context-manifest",
                "ai-context-field-catalog",
                "ai-task-profile"
        )));
        assertTrue(catalog.getContracts().stream().allMatch(item -> item.getJsonSchemaRef().startsWith("dataspec://contracts/")));
        assertTrue(catalog.getContracts().stream().allMatch(item -> item.getStableFields() != null && !item.getStableFields().isEmpty()));
        assertTrue(catalog.getContracts().stream().allMatch(item -> item.getDeprecatedFields() != null));
    }

    @Test
    void detailExposesJsonSchemaStableFieldsAndExamples() {
        SchemaContract contract = service.getContract("lint-result");

        assertEquals("lint-result", contract.getContractId());
        assertEquals("1.0", contract.getSchemaVersion());
        assertTrue(contract.getStableFields().contains("fixedSql"));
        assertTrue(contract.getStableFields().contains("dialectDiagnostics[]"));
        assertEquals("object", contract.getJsonSchema().get("type"));
        assertNotNull(contract.getJsonSchema().get("properties"));
        assertFalse(contract.getExamples().isEmpty());
        assertNotNull(contract.getCompatibility());

        SchemaContract evidence = service.getContract("ai-evidence-package");
        assertTrue(evidence.getStableFields().contains("validationSummary"));
        assertTrue(evidence.getStableFields().contains("suggestedCommands[]"));

        SchemaContract fieldCatalog = service.getContract("ai-context-field-catalog");
        assertTrue(fieldCatalog.getStableFields().contains("usageExamples[]"));
        assertTrue(fieldCatalog.getStableFields().contains("usageExampleSummary"));
        assertTrue(fieldCatalog.getJsonSchema().get("properties").toString().contains("usageExamples"));

        SchemaContract merge = service.getContract("standard-field-merge");
        assertTrue(merge.getStableFields().contains("risks[].blocking"));
        assertTrue(merge.getStableFields().contains("rollbackHints[].targetPath"));
        assertTrue(merge.getStableFields().contains("applied"));
        assertTrue(merge.getStableFields().contains("preview"));
        assertTrue(merge.getJsonSchema().get("properties").toString().contains("standard_field_merge_preview"));
        assertTrue(merge.getJsonSchema().get("properties").toString().contains("applied"));
        assertTrue(merge.getJsonSchema().get("properties").toString().contains("preview"));
        assertTrue(merge.getExamples().toString().contains("standard_field_merge_result"));
    }

    @Test
    void unknownContractReturnsSupportedIdsInException() {
        BizException error = assertThrows(BizException.class, () -> service.getContract("missing-contract"));

        assertEquals(404, error.getCode());
        assertTrue(error.getMessage().contains("missing-contract"));
        assertTrue(error.getMessage().contains("field"));
        assertTrue(error.getMessage().contains("lint-result"));
    }

    @Test
    void manifestSummaryPointsToRegistryFileAndCoreIds() {
        var summary = service.manifestSummary();

        assertEquals(1, summary.get("schemaVersion"));
        assertEquals("2026.07.05", summary.get("registryVersion"));
        assertEquals(".dataspec/schema-registry.json", summary.get("file"));
        assertEquals(service.requiredContractIds(), summary.get("contractIds"));
    }
}
