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
        assertEquals("2026.07.10", catalog.getRegistryVersion());
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
                "sql-rule-debug-result",
                "ai-evidence-package",
                "ai-context-manifest",
                "ai-context-field-catalog",
                "ai-task-profile",
                "standard-reference-resolution",
                "ai-output-post-check-result"
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

        SchemaContract debug = service.getContract("sql-rule-debug-result");
        assertTrue(debug.getStableFields().contains("rules[].matchTrace[]"));
        assertTrue(debug.getJsonSchema().get("properties").toString().contains("UNPARSED"));
        assertTrue(debug.getExamples().toString().contains("sql-rule-debug@1"));

        SchemaContract evidence = service.getContract("ai-evidence-package");
        assertTrue(evidence.getStableFields().contains("validationSummary"));
        assertTrue(evidence.getStableFields().contains("postCheckSummary"));
        assertTrue(evidence.getStableFields().contains("postCheckSummary.blockingRefs[]"));
        assertTrue(evidence.getJsonSchema().get("properties").toString().contains("postCheckSummary"));
        assertTrue(evidence.getStableFields().contains("suggestedCommands[]"));

        SchemaContract field = service.getContract("field");
        assertTrue(field.getStableFields().contains("stableRef"));
        assertTrue(field.getStableFields().contains("canonicalRef"));
        assertTrue(field.getStableFields().contains("aliasHistory[]"));
        assertTrue(field.getStableFields().contains("replacementRef"));

        SchemaContract fieldCatalog = service.getContract("ai-context-field-catalog");
        assertTrue(fieldCatalog.getStableFields().contains("usageExamples[]"));
        assertTrue(fieldCatalog.getStableFields().contains("usageExampleSummary"));
        assertTrue(fieldCatalog.getStableFields().contains("fields[].stableRef"));
        assertTrue(fieldCatalog.getStableFields().contains("fields[].canonicalRef"));
        assertTrue(fieldCatalog.getStableFields().contains("fields[].aliasHistory[]"));
        assertTrue(fieldCatalog.getStableFields().contains("enums[].stableRef"));
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
    void detailDescribesStableReferenceAndPostCheckContracts() {
        SchemaContract reference = service.getContract("standard-reference-resolution");
        assertEquals("standard-reference-resolution", reference.getContractId());
        assertTrue(reference.getStableFields().contains("results[].resolutionStatus"));
        assertTrue(reference.getStableFields().contains("results[].stableRef"));
        assertTrue(reference.getStableFields().contains("results[].canonicalRef"));
        assertTrue(reference.getJsonSchema().get("properties").toString().contains("resolutionStatus"));
        assertTrue(reference.getJsonSchema().get("properties").toString().contains("CURRENT"));
        assertTrue(reference.getJsonSchema().get("properties").toString().contains("CROSS_PROJECT"));
        assertTrue(reference.getJsonSchema().get("properties").toString().contains("secret-safe"));
        assertTrue(reference.getExamples().toString().contains("field:1:100"));

        SchemaContract postCheck = service.getContract("ai-output-post-check-result");
        assertEquals("ai-output-post-check-result", postCheck.getContractId());
        assertTrue(postCheck.getStableFields().contains("status"));
        assertTrue(postCheck.getStableFields().contains("safeToUse"));
        assertTrue(postCheck.getStableFields().contains("issues[].severity"));
        assertTrue(postCheck.getStableFields().contains("resolvedRefs[]"));
        assertTrue(postCheck.getStableFields().contains("nextActions[]"));
        assertTrue(postCheck.getJsonSchema().get("properties").toString().contains("PASS"));
        assertTrue(postCheck.getJsonSchema().get("properties").toString().contains("WARN"));
        assertTrue(postCheck.getJsonSchema().get("properties").toString().contains("FAIL"));
        assertTrue(postCheck.getJsonSchema().get("properties").toString().contains("safeToUse"));
        assertTrue(postCheck.getJsonSchema().get("properties").toString().contains("secret-safe"));
        assertTrue(postCheck.getExamples().toString().contains("safeToUse"));
    }

    @Test
    void detailUsesActualStableReferenceAndPostCheckEnums() {
        String referenceSchema = service.getContract("standard-reference-resolution")
                .getJsonSchema()
                .get("properties")
                .toString();
        assertTrue(referenceSchema.contains("HIGH"));
        assertTrue(referenceSchema.contains("MEDIUM"));
        assertTrue(referenceSchema.contains("LOW"));
        assertFalse(referenceSchema.contains("EXACT"));
        assertFalse(referenceSchema.contains("DERIVED"));
        assertFalse(referenceSchema.contains("NONE"));

        String postCheckSchema = service.getContract("ai-output-post-check-result")
                .getJsonSchema()
                .get("properties")
                .toString();
        assertTrue(postCheckSchema.contains("totalRefCount"));
        assertTrue(postCheckSchema.contains("currentCount"));
        assertTrue(postCheckSchema.contains("staleCount"));
        assertTrue(postCheckSchema.contains("unknownCount"));
        assertTrue(postCheckSchema.contains("ambiguousCount"));
        assertTrue(postCheckSchema.contains("crossProjectCount"));
        assertTrue(postCheckSchema.contains("issueCount"));
        assertFalse(postCheckSchema.contains("totalRefs"));
        assertFalse(postCheckSchema.contains("blockingIssueCount"));
        assertFalse(postCheckSchema.contains("INFO"));
    }

    @Test
    void evidenceContractIncludesAiTaskRunSourceTypeAndPostCheckSummary() {
        SchemaContract evidence = service.getContract("ai-evidence-package");
        String schema = evidence.getJsonSchema().get("properties").toString();

        assertTrue(schema.contains("AI_TASK_RUN"));
        assertTrue(evidence.getStableFields().contains("postCheckSummary"));
        assertTrue(evidence.getStableFields().contains("postCheckSummary.suggestedCheckCommand"));
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
        assertEquals("2026.07.10", summary.get("registryVersion"));
        assertEquals(".dataspec/schema-registry.json", summary.get("file"));
        assertEquals(service.requiredContractIds(), summary.get("contractIds"));
    }
}
