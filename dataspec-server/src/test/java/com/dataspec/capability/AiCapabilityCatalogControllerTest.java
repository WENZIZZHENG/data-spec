package com.dataspec.capability;

import com.dataspec.capability.controller.AiCapabilityCatalogController;
import com.dataspec.capability.model.AiCapabilityCatalog;
import com.dataspec.capability.model.AiCapabilityDiagnostic;
import com.dataspec.capability.model.AiCapabilityEntry;
import com.dataspec.capability.model.AiWriteSafetyMetadata;
import com.dataspec.capability.service.AiCapabilityCatalogService;
import com.dataspec.capability.service.impl.AiCapabilityCatalogServiceImpl;
import com.dataspec.common.result.R;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiCapabilityCatalogControllerTest {

    @Test
    void catalogDelegatesToServiceWithProjectId() {
        RecordingCapabilityCatalogService service = new RecordingCapabilityCatalogService();
        AiCapabilityCatalogController controller = new AiCapabilityCatalogController(service);

        R<AiCapabilityCatalog> response = controller.catalog(7L);

        assertEquals(200, response.getCode());
        assertEquals(7L, service.lastCatalogProjectId);
        assertEquals(7L, response.getData().projectId());
        assertTrue(service.catalogCalled);
    }

    @Test
    void capabilityDelegatesToServiceWithIdAndProjectId() {
        RecordingCapabilityCatalogService service = new RecordingCapabilityCatalogService();
        AiCapabilityCatalogController controller = new AiCapabilityCatalogController(service);

        R<AiCapabilityEntry> response = controller.capability("lint-sql", 7L);

        assertEquals(200, response.getCode());
        assertEquals("lint-sql", service.lastCapabilityId);
        assertEquals(7L, service.lastCapabilityProjectId);
        assertEquals("lint-sql", response.getData().id());
    }

    private static class RecordingCapabilityCatalogService implements AiCapabilityCatalogService {
        private boolean catalogCalled;
        private Long lastCatalogProjectId;
        private String lastCapabilityId;
        private Long lastCapabilityProjectId;

        @Override
        public AiCapabilityCatalog getCatalog(Long projectId) {
            catalogCalled = true;
            lastCatalogProjectId = projectId;
            return new AiCapabilityCatalog(
                    AiCapabilityCatalogServiceImpl.KIND,
                    1,
                    "2026.06.28",
                    LocalDateTime.now(),
                    projectId,
                    List.of(capability("lint-sql")),
                    List.of("lint-sql"),
                    List.of("run doctor"),
                    List.of(new AiCapabilityDiagnostic("CATALOG_READY", "pass", "ready", "continue"))
            );
        }

        @Override
        public AiCapabilityEntry getCapability(String capabilityId, Long projectId) {
            lastCapabilityId = capabilityId;
            lastCapabilityProjectId = projectId;
            return capability(capabilityId);
        }

        private AiCapabilityEntry capability(String id) {
            return new AiCapabilityEntry(
                    id,
                    "sql",
                    "SQL 校验",
                    "校验 SQL",
                    "AVAILABLE",
                    "stable-ai",
                    true,
                    "WRITES_DATASPEC_RECORD",
                    List.of("projectId", "sql"),
                    List.of(),
                    List.of("lint-result"),
                    List.of("POST /api/lint"),
                    List.of("dataspec lint <file.sql> --project <id> --format json"),
                    List.of(),
                    List.of("lint_sql"),
                    List.of("/sql-lint"),
                    List.of("lint-result"),
                    List.of("review-pr-sql"),
                    List.of("sql-fix"),
                    List.of(),
                    List.of("run doctor"),
                    List.of("fix errors"),
                    new AiWriteSafetyMetadata(false, true, false, false, false, List.of(), List.of("fix errors")),
                    "README.md#sql-规范闭环"
            );
        }
    }
}
