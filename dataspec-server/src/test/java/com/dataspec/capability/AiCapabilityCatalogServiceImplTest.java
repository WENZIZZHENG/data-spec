package com.dataspec.capability;

import com.dataspec.capability.model.AiCapabilityCatalog;
import com.dataspec.capability.model.AiCapabilityEntry;
import com.dataspec.capability.service.impl.AiCapabilityCatalogServiceImpl;
import com.dataspec.common.exception.BizException;
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
        assertEquals("2026.06.28", catalog.catalogVersion());
        assertNotNull(catalog.generatedAt());
        assertFalse(catalog.capabilities().isEmpty());
        assertEquals(catalog.requiredCapabilityIds(), catalog.capabilities().stream().map(AiCapabilityEntry::id).toList());
        assertTrue(catalog.requiredCapabilityIds().containsAll(List.of(
                "capability-catalog",
                "doctor",
                "export-ai-context",
                "lint-sql",
                "search-fields",
                "suggest-fields",
                "generate-ddl",
                "reverse-import",
                "coverage-report",
                "schema-registry",
                "export-evidence-package",
                "workflow-recipes",
                "ai-task-profiles",
                "domain-starter-kits",
                "session-bootstrap"
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
