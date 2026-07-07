package com.dataspec.capability;

import com.dataspec.capability.model.AiCapabilityCatalog;
import com.dataspec.capability.model.AiCapabilityEntry;
import com.dataspec.capability.model.VersionCompatibilityResponse;
import com.dataspec.capability.service.impl.AiCapabilityCatalogServiceImpl;
import com.dataspec.common.exception.BizException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiCapabilityCatalogServiceImplTest {

    private final AiCapabilityCatalogServiceImpl service = new AiCapabilityCatalogServiceImpl();
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Test
    void catalogExposesCoreAiCapabilities() {
        AiCapabilityCatalog catalog = service.getCatalog(null);

        assertEquals(AiCapabilityCatalogServiceImpl.KIND, catalog.kind());
        assertEquals(1, catalog.schemaVersion());
        assertEquals("2026.07.07", catalog.catalogVersion());
        assertNotNull(catalog.generatedAt());
        assertFalse(catalog.capabilities().isEmpty());
        assertEquals(catalog.requiredCapabilityIds(), catalog.capabilities().stream().map(AiCapabilityEntry::id).toList());
        assertTrue(catalog.requiredCapabilityIds().containsAll(List.of(
                "capability-catalog",
                "doctor",
                "export-ai-context",
                "lint-sql",
                "sql-rule-debugger",
                "search-fields",
                "suggest-fields",
                "merge-standard-fields",
                "generate-ddl",
                "reverse-import",
                "project-backup-restore",
                "standard-reuse-packs",
                "ai-batch-sql-lint",
                "coverage-report",
                "schema-registry",
                "export-evidence-package",
                "workflow-recipes",
                "ai-task-profiles",
                "domain-starter-kits",
                "session-bootstrap",
                "standard-evidence"
        )));
    }

    @Test
    void projectDiagnosticsDistinguishGlobalAndProjectCatalogs() {
        AiCapabilityCatalog globalCatalog = service.getCatalog(null);

        assertEquals("MISSING_PROJECT", globalCatalog.diagnostics().get(0).code());
        assertEquals("warn", globalCatalog.diagnostics().get(0).status());
        assertTrue(globalCatalog.recommendedFirstActions().toString().contains("projectId"));

        AiCapabilityCatalog projectCatalog = service.getCatalog(1L);

        assertEquals(1L, projectCatalog.projectId());
        assertEquals("CATALOG_READY", projectCatalog.diagnostics().get(0).code());
        assertEquals("pass", projectCatalog.diagnostics().get(0).status());
        assertTrue(projectCatalog.recommendedFirstActions().toString().contains("--project 1"));
    }

    @Test
    void versionCompatibilityEvaluatesClientVersionsWithoutSecrets() throws Exception {
        VersionCompatibilityResponse compatible = service.getVersionCompatibility("cli", "0.1.0");

        assertEquals("dataspec-version-compatibility", compatible.kind());
        assertEquals(1, compatible.schemaVersion());
        assertEquals("0.1.0-SNAPSHOT", compatible.serverVersion());
        assertTrue(compatible.apiSchemaHash().startsWith("sha256:"));
        assertEquals("0.1.0", compatible.minCliVersion());
        assertEquals("COMPATIBLE", compatible.compatibility().status());
        assertTrue(compatible.compatibility().compatible());
        assertTrue(compatible.supportedCapabilities().stream()
                .anyMatch(capability -> "version-compatibility".equals(capability.id())));

        VersionCompatibilityResponse incompatible = service.getVersionCompatibility("cli", "0.0.1");
        assertEquals("INCOMPATIBLE", incompatible.compatibility().status());
        assertFalse(incompatible.compatibility().compatible());
        assertTrue(incompatible.compatibility().reasons().toString().contains("0.1.0"));

        VersionCompatibilityResponse unknown = service.getVersionCompatibility("mcp", "legacy");
        assertEquals("UNKNOWN", unknown.compatibility().status());
        assertTrue(unknown.compatibility().compatible());
        assertTrue(unknown.compatibility().nextActions().toString().contains("compat check"));

        VersionCompatibilityResponse defaulted = service.getVersionCompatibility("cli", null);
        assertEquals("UNKNOWN", defaulted.compatibility().status());
        assertTrue(defaulted.compatibility().compatible());

        String json = objectMapper.writeValueAsString(compatible);
        assertFalse(json.contains("Authorization"));
        assertFalse(json.contains("password"));
        assertFalse(json.contains("jdbc:"));
        assertFalse(json.contains("dsn"));
    }

    @Test
    void catalogExposesVersionCompatibilityCapability() {
        AiCapabilityCatalog catalog = service.getCatalog(null);
        AiCapabilityEntry entry = service.getCapability("version_compatibility", null);
        JsonNode safety = objectMapper.valueToTree(entry).path("safety");

        assertTrue(catalog.requiredCapabilityIds().contains("version-compatibility"));
        assertTrue(catalog.recommendedFirstActions().toString().contains("compat check"));
        assertEquals("version-compatibility", entry.id());
        assertEquals("READ_ONLY", entry.writeRisk());
        assertTrue(entry.apiEndpoints().contains("GET /api/capabilities/version"));
        assertTrue(entry.cliCommands().contains("dataspec compat check --format json"));
        assertTrue(entry.mcpResources().contains("dataspec://version-compatibility"));
        assertTrue(safety.path("readOnly").asBoolean(false));
        assertFalse(safety.path("writesProject").asBoolean(true));
    }

    @Test
    void showCapabilityNormalizesIdAndExposesSurfaces() {
        AiCapabilityEntry entry = service.getCapability("lint_sql", 1L);

        assertEquals("lint-sql", entry.id());
        assertEquals("WRITES_DATASPEC_RECORD", entry.writeRisk());
        assertTrue(entry.requiresProject());
        assertTrue(entry.apiEndpoints().contains("POST /api/lint"));
        assertTrue(entry.cliCommands().toString().contains("dataspec lint"));
        assertTrue(entry.mcpTools().contains("lint_sql"));
        assertTrue(entry.outputContracts().contains("lint-result"));
        assertFalse(entry.preflightChecks().isEmpty());
        assertFalse(entry.nextActions().isEmpty());
    }

    @Test
    void sqlRuleDebuggerCapabilityIsReadOnlyAndExposesDebugSurfaces() {
        AiCapabilityEntry entry = service.getCapability("sql_rule_debugger", 1L);
        JsonNode safety = objectMapper.valueToTree(entry).path("safety");

        assertEquals("sql-rule-debugger", entry.id());
        assertEquals("READ_ONLY", entry.writeRisk());
        assertTrue(entry.requiresProject());
        assertTrue(entry.apiEndpoints().contains("POST /api/lint/debug"));
        assertTrue(entry.cliCommands().stream().anyMatch(command -> command.contains("lint-debug")));
        assertTrue(entry.outputContracts().contains("sql-rule-debug-result"));
        assertTrue(entry.preflightChecks().stream().anyMatch(check -> check.contains("不会保存 SQL 检查记录")));
        assertTrue(safety.path("readOnly").asBoolean(false));
        assertFalse(safety.path("writesProject").asBoolean(true));
        assertFalse(safety.path("requiresIdempotencyKey").asBoolean(true));
    }

    @Test
    void standardEvidenceCapabilityIsReadOnlyAndDiscoverable() {
        AiCapabilityCatalog catalog = service.getCatalog(1L);
        AiCapabilityEntry entry = service.getCapability("standard_evidence", 1L);
        JsonNode safety = objectMapper.valueToTree(entry).path("safety");
        VersionCompatibilityResponse version = service.getVersionCompatibility("cli", "0.1.0");

        assertTrue(catalog.requiredCapabilityIds().contains("standard-evidence"));
        assertEquals("standard-evidence", entry.id());
        assertEquals("evidence", entry.category());
        assertEquals("READ_ONLY", entry.writeRisk());
        assertTrue(entry.requiresProject());
        assertEquals(List.of("projectId", "subjectType", "subjectId"), entry.requiredInputs());
        assertTrue(entry.optionalInputs().isEmpty());
        assertEquals(List.of("cross-source-standard-evidence-view"), entry.outputContracts());
        assertEquals(List.of("GET /api/standard-evidence"), entry.apiEndpoints());
        assertEquals(List.of(), entry.cliCommands());
        assertEquals(List.of(), entry.mcpResources());
        assertEquals(List.of(), entry.mcpTools());
        assertTrue(entry.examples().stream().anyMatch(example ->
                "GET /api/standard-evidence?projectId=1&subjectType=FIELD&subjectId=10"
                        .equals(example.request())));
        assertTrue(entry.preflightChecks().stream().anyMatch(check -> check.contains("FIELD")));
        assertTrue(entry.nextActions().stream().anyMatch(action -> action.contains("/api/standard-evidence")));
        assertTrue(safety.path("readOnly").asBoolean(false));
        assertFalse(safety.path("writesProject").asBoolean(true));
        assertFalse(safety.path("requiresIdempotencyKey").asBoolean(true));
        assertTrue(safety.path("sensitiveInputs").isArray());
        assertTrue(safety.path("nextActions").toString().contains("raw SQL"));
        assertTrue(version.supportedCapabilities().stream()
                .anyMatch(capability -> "standard-evidence".equals(capability.id())));
    }

    @Test
    void catalogSerializesSafetyMetadataForEveryCapability() {
        JsonNode catalog = objectMapper.valueToTree(service.getCatalog(1L));

        for (JsonNode capability : catalog.path("capabilities")) {
            String id = capability.path("id").asText();
            JsonNode safety = capability.path("safety");

            assertTrue(safety.isObject(), id + " 应输出 safety metadata");
            assertTrue(safety.has("readOnly"), id + " 缺少 readOnly");
            assertTrue(safety.has("writesProject"), id + " 缺少 writesProject");
            assertTrue(safety.has("requiresDryRun"), id + " 缺少 requiresDryRun");
            assertTrue(safety.has("supportsUndo"), id + " 缺少 supportsUndo");
            assertTrue(safety.has("requiresIdempotencyKey"), id + " 缺少 requiresIdempotencyKey");
            assertTrue(safety.path("sensitiveInputs").isArray(), id + " sensitiveInputs 必须是数组");
            assertTrue(safety.path("nextActions").isArray(), id + " nextActions 必须是数组");
        }
    }

    @Test
    void highRiskCapabilitySafetyRequiresPreviewAndIdempotency() {
        JsonNode reverseImport = objectMapper.valueToTree(service.getCapability("reverse_import", 1L));
        JsonNode safety = reverseImport.path("safety");

        assertFalse(safety.path("readOnly").asBoolean(true));
        assertTrue(safety.path("writesProject").asBoolean(false));
        assertTrue(safety.path("requiresDryRun").asBoolean(false));
        assertTrue(safety.path("requiresIdempotencyKey").asBoolean(false));
        assertTrue(safety.path("sensitiveInputs").toString().contains("databasePassword"));
        assertTrue(safety.path("nextActions").toString().contains("preview"));
    }

    @Test
    void catalogSafetyMatchesEnforcedWriteOperations() {
        JsonNode restoreSafety = objectMapper.valueToTree(service.getCapability("project_backup_restore", 1L)).path("safety");
        JsonNode reusePackSafety = objectMapper.valueToTree(service.getCapability("standard_reuse_packs", 1L)).path("safety");
        JsonNode aiBatchSafety = objectMapper.valueToTree(service.getCapability("ai_batch_sql_lint", 1L)).path("safety");
        JsonNode mergeSafety = objectMapper.valueToTree(service.getCapability("merge_standard_fields", 1L)).path("safety");
        JsonNode starterKitSafety = objectMapper.valueToTree(service.getCapability("domain_starter_kits", 1L)).path("safety");

        assertTrue(restoreSafety.path("requiresDryRun").asBoolean(false));
        assertTrue(restoreSafety.path("requiresIdempotencyKey").asBoolean(false));
        assertTrue(restoreSafety.path("nextActions").toString().contains("Idempotency-Key"));
        assertFalse(reusePackSafety.path("requiresDryRun").asBoolean(true));
        assertFalse(mergeSafety.path("requiresDryRun").asBoolean(true));
        assertFalse(starterKitSafety.path("requiresDryRun").asBoolean(true));
        assertTrue(aiBatchSafety.path("requiresIdempotencyKey").asBoolean(false));
        assertFalse(aiBatchSafety.path("readOnly").asBoolean(true));
        assertFalse(service.getCapability("standard_reuse_packs", 1L).optionalInputs().contains("dryRunToken"));
        assertTrue(service.getCapability("ai_batch_sql_lint", 1L).cliCommands().isEmpty());
    }

    @Test
    void reverseImportCapabilityExposesMetadataScanPlanSurfaces() {
        AiCapabilityEntry entry = service.getCapability("reverse_import", 1L);

        assertTrue(entry.apiEndpoints().contains("POST /api/reverse-import/database/scan"));
        assertTrue(entry.apiEndpoints().contains("POST /api/reverse-import/database/browser"));
        assertTrue(entry.optionalInputs().containsAll(List.of("schemaName", "tableNames", "pageSize", "cursor", "cancel", "metadataCacheMode")));
        assertTrue(entry.outputContracts().contains("database-metadata-scan-result"));
        assertTrue(entry.outputContracts().contains("database-metadata-browser"));
        assertTrue(entry.outputContracts().contains("database-metadata-cache-info"));
        assertTrue(entry.contractIds().contains("database-metadata-scan-plan"));
        assertTrue(entry.contractIds().contains("db-metadata-incremental-cache"));
        assertTrue(entry.preflightChecks().stream().anyMatch(check -> check.contains("metadataFingerprint")));
        assertTrue(entry.nextActions().stream().anyMatch(action -> action.contains("metadataCacheMode=REFRESH")));
    }

    @Test
    void mergeStandardFieldsCapabilityExposesPreviewAndApplySurfaces() {
        AiCapabilityEntry entry = service.getCapability("merge_standard_fields", 1L);

        assertEquals("merge-standard-fields", entry.id());
        assertEquals("WRITES_DATASPEC_STANDARD", entry.writeRisk());
        assertTrue(entry.apiEndpoints().contains("POST /api/fields/merge/preview"));
        assertTrue(entry.apiEndpoints().contains("POST /api/fields/merge/apply"));
        assertTrue(entry.outputContracts().contains("standard-field-merge"));
        assertTrue(entry.preflightChecks().stream().anyMatch(check -> check.contains("合并原因")));
    }

    @Test
    void unknownCapabilitySuggestsListingCatalog() {
        BizException error = assertThrows(BizException.class, () -> service.getCapability("missing", null));

        assertEquals(404, error.getCode());
        assertTrue(error.getMessage().contains("missing"));
        assertTrue(error.getMessage().contains("/api/capabilities"));
    }

    @Test
    void catalogDoesNotExposeSecretsOrDatabaseRows() throws Exception {
        String json = objectMapper.writeValueAsString(service.getCatalog(1L));

        assertFalse(json.contains("Authorization"));
        assertFalse(json.contains("Bearer "));
        assertFalse(json.contains("password"));
        assertFalse(json.contains("tokenHash"));
        assertFalse(json.contains("jdbc:postgresql://"));
        assertFalse(json.contains("jdbc:mysql://"));
        assertFalse(json.contains("source database rows"));
    }

    @Test
    void catalogExposesSessionBootstrapSurfaces() {
        AiCapabilityEntry entry = service.getCapability("session_bootstrap", 1L);

        assertEquals("session-bootstrap", entry.id());
        assertEquals("READ_ONLY", entry.writeRisk());
        assertTrue(entry.apiEndpoints().contains("GET /api/bootstrap/session"));
        assertTrue(entry.cliCommands().stream().anyMatch(command -> command.contains("bootstrap")));
        assertTrue(entry.mcpResources().contains("dataspec://project/<id>/session-bootstrap"));
        assertTrue(entry.mcpTools().contains("get_session_bootstrap"));
        assertTrue(entry.preflightChecks().stream().anyMatch(check -> check.contains("不执行")));
    }
}
