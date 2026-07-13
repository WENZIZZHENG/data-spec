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
        assertEquals("2026.07.11", catalog.getRegistryVersion());
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
                "ai-output-post-check-result",
                "standard-query-dsl-request",
                "standard-query-dsl-result",
                "business-object-standard",
                "table-structure-standard",
                "table-relation-hint",
                "table-index-standard",
                "table-foreign-key-standard",
                "table-policy-standard",
                "ai-context-table-standards",
                "standard-test-data-package",
                "consumer-compatibility-suite",
                "consumer-compatibility-adapter-result",
                "consumer-compatibility-breaking-rule"
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
        assertTrue(evidence.getStableFields().contains("source.evidenceRef"));
        assertTrue(evidence.getJsonSchema().get("properties").toString().contains("postCheckSummary"));
        assertTrue(evidence.getJsonSchema().get("properties").toString().contains("evidenceRef"));
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
        assertTrue(postCheck.getJsonSchema().get("properties").toString().contains("MISSING_EVIDENCE_REFERENCE"));
        assertTrue(postCheck.getJsonSchema().get("properties").toString().contains("CROSS_PROJECT_EVIDENCE_REFERENCE"));
        assertTrue(postCheck.getJsonSchema().get("properties").toString().contains("UNVERIFIABLE_EVIDENCE_REFERENCE"));
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
    void detailDescribesStandardQueryDslContracts() {
        SchemaContract request = service.getContract("standard-query-dsl-request");
        assertEquals("standard-query-dsl-request", request.getContractId());
        assertTrue(request.getStableFields().contains("target"));
        assertTrue(request.getStableFields().contains("filters[].field"));
        assertTrue(request.getStableFields().contains("filters[].op"));
        assertTrue(request.getStableFields().contains("limit"));
        String requestSchema = request.getJsonSchema().get("properties").toString();
        assertTrue(requestSchema.contains("FIELD"));
        assertTrue(requestSchema.contains("category"));
        assertTrue(requestSchema.contains("updatedSince"));
        assertTrue(requestSchema.contains("secret-safe"));
        assertTrue(requestSchema.contains("description"));

        SchemaContract result = service.getContract("standard-query-dsl-result");
        assertEquals("standard-query-dsl-result", result.getContractId());
        assertTrue(result.getStableFields().contains("normalizedQuery"));
        assertTrue(result.getStableFields().contains("querySummary"));
        assertTrue(result.getStableFields().contains("appliedFilters[]"));
        assertTrue(result.getStableFields().contains("ignoredFilters[]"));
        assertTrue(result.getStableFields().contains("nextQueryHints[]"));
        String resultSchema = result.getJsonSchema().get("properties").toString();
        assertTrue(resultSchema.contains("resultCount"));
        assertTrue(resultSchema.contains("returnedCount"));
        assertTrue(resultSchema.contains("truncated"));
        assertTrue(resultSchema.contains("supportedFields"));
        assertTrue(resultSchema.contains("bounds"));
        assertTrue(resultSchema.contains("secret-safety"));
    }

    @Test
    void detailDescribesBusinessObjectTableStandardContracts() {
        SchemaContract businessObject = service.getContract("business-object-standard");
        assertTrue(businessObject.getStableFields().contains("objectKey"));
        assertTrue(businessObject.getStableFields().contains("foreignKeyHints[]"));
        assertTrue(businessObject.getJsonSchema().get("properties").toString().contains("secret"));

        SchemaContract structure = service.getContract("table-structure-standard");
        assertTrue(structure.getStableFields().contains("primaryKey"));
        assertTrue(structure.getStableFields().contains("foreignKeys[]"));
        assertTrue(structure.getJsonSchema().get("properties").toString().contains("checkHints"));
        assertTrue(structure.getJsonSchema().get("properties").toString().contains("不拼 raw SQL"));

        SchemaContract tableStandards = service.getContract("ai-context-table-standards");
        assertTrue(tableStandards.getStableFields().contains("contextScope.scope"));
        assertTrue(tableStandards.getStableFields().contains("templates[].structure"));
        String schema = tableStandards.getJsonSchema().get("properties").toString();
        assertTrue(schema.contains("business-object"));
        assertTrue(schema.contains("table-template"));
        assertTrue(schema.contains("truncated"));

        SchemaContract index = service.getContract("table-index-standard");
        String indexSchema = index.getJsonSchema().get("properties").toString();
        assertTrue(indexSchema.contains("btree"));
        assertFalse(indexSchema.contains("gin"));

        SchemaContract foreignKey = service.getContract("table-foreign-key-standard");
        assertTrue(foreignKey.getStableFields().contains("advisoryOnly"));
        assertFalse(foreignKey.getStableFields().contains("advisory"));
        String foreignKeySchema = foreignKey.getJsonSchema().get("properties").toString();
        assertTrue(foreignKeySchema.contains("NO ACTION"));
        assertTrue(foreignKeySchema.contains("SET NULL"));
        assertTrue(foreignKeySchema.contains("advisoryOnly"));
        assertFalse(foreignKeySchema.contains("NO_ACTION"));
        assertFalse(foreignKeySchema.contains("SET_NULL"));
    }

    @Test
    void detailDescribesTestDataAndConsumerCompatibilityContracts() {
        SchemaContract testData = service.getContract("standard-test-data-package");
        assertTrue(testData.getStableFields().contains("testDataCases[]"));
        assertTrue(testData.getStableFields().contains("testDataCases[].value"));
        assertTrue(testData.getStableFields().contains("testDataCases[].reason"));
        assertTrue(testData.getStableFields().contains("testDataCases[].sourceRefs[]"));
        assertTrue(testData.getStableFields().contains("testDataCases[].requiresBusinessReview"));
        assertTrue(testData.getStableFields().contains("seedProfiles[]"));
        assertTrue(testData.getStableFields().contains("coverageReport"));
        assertTrue(testData.getStableFields().contains("safety.readOnly"));
        assertTrue(testData.getStableFields().contains("safety.writesProject"));
        assertTrue(testData.getStableFields().contains("safety.writesBusinessRepo"));
        assertTrue(testData.getStableFields().contains("safety.containsRealBusinessRows"));
        assertTrue(testData.getStableFields().contains("safety.externalNetworkUsed"));
        assertTrue(testData.getStableFields().contains("safety.externalLlmUsed"));
        String testDataSchema = testData.getJsonSchema().get("properties").toString();
        assertTrue(testDataSchema.contains("安全元数据"));
        assertTrue(testDataSchema.contains("不包含真实业务数据行"));
        assertTrue(testDataSchema.contains("SQL seed 草稿"));
        assertTrue(testDataSchema.contains("nextActions"));
        assertTrue(testDataSchema.contains("standardFieldCount"));
        assertTrue(testDataSchema.contains("selectedFieldCount"));
        assertTrue(testDataSchema.contains("enumValueCount"));
        assertTrue(testDataSchema.contains("payloadId"));
        assertTrue(testDataSchema.contains("objectScenario"));
        assertTrue(testDataSchema.contains("coverageLevel"));
        assertTrue(testDataSchema.contains("sensitiveInputCategories"));
        assertFalse(testDataSchema.contains("样例来源；第一版只能来自标准元数据或内置 fallback"));
        assertFalse(testDataSchema.contains("fieldCount"));
        assertFalse(testDataSchema.contains("dialectNote"));
        assertFalse(testDataSchema.contains("contentType"));
        assertFalse(testDataSchema.contains("supportedCaseCount"));
        assertFalse(testData.getExamples().toString().contains("Authorization: Bearer raw-secret"));
        assertFalse(testData.getExamples().toString().contains("jdbc:postgresql://user:pass@"));

        SchemaContract suite = service.getContract("consumer-compatibility-suite");
        assertTrue(suite.getStableFields().contains("adapterResults[]"));
        assertTrue(suite.getStableFields().contains("breakingRules[]"));
        assertTrue(suite.getStableFields().contains("diagnostics[]"));
        String suiteSchema = suite.getJsonSchema().get("properties").toString();
        assertTrue(suiteSchema.contains("本地只读"));
        assertTrue(suiteSchema.contains("adapterResults"));
        assertTrue(suiteSchema.contains("contractRefs"));
        assertTrue(suiteSchema.contains("stableFields"));
        assertTrue(suiteSchema.contains("nextActions"));

        SchemaContract adapterResult = service.getContract("consumer-compatibility-adapter-result");
        assertTrue(adapterResult.getStableFields().contains("adapterId"));
        assertTrue(adapterResult.getStableFields().contains("required"));
        assertTrue(adapterResult.getStableFields().contains("contractRefs[]"));
        assertTrue(adapterResult.getStableFields().contains("stableFields[]"));
        assertTrue(adapterResult.getStableFields().contains("checkedStableFields[]"));
        assertTrue(adapterResult.getStableFields().contains("additiveFields[]"));
        assertTrue(adapterResult.getJsonSchema().get("properties").toString().contains("COMPATIBLE"));
        assertTrue(adapterResult.getJsonSchema().get("properties").toString().contains("BREAKING"));

        SchemaContract breakingRule = service.getContract("consumer-compatibility-breaking-rule");
        assertTrue(breakingRule.getStableFields().contains("ruleId"));
        assertTrue(breakingRule.getStableFields().contains("migrationHint"));
        assertTrue(breakingRule.getJsonSchema().get("properties").toString().contains("migration hint"));
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
        assertEquals("2026.07.11", summary.get("registryVersion"));
        assertEquals(".dataspec/schema-registry.json", summary.get("file"));
        assertEquals(service.requiredContractIds(), summary.get("contractIds"));
    }
}
