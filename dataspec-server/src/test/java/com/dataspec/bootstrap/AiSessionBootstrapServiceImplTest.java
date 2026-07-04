package com.dataspec.bootstrap;

import com.dataspec.bootstrap.model.AiSessionBootstrap;
import com.dataspec.bootstrap.service.impl.AiSessionBootstrapServiceImpl;
import com.dataspec.capability.service.impl.AiCapabilityCatalogServiceImpl;
import com.dataspec.standard.dto.StandardSnapshotInfo;
import com.dataspec.standard.service.StandardSnapshotService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiSessionBootstrapServiceImplTest {

    private final StandardSnapshotService standardSnapshotService = mock(StandardSnapshotService.class);
    private final AiSessionBootstrapServiceImpl service = new AiSessionBootstrapServiceImpl(
            new AiCapabilityCatalogServiceImpl(),
            standardSnapshotService
    );
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Test
    void buildReadyBootstrapWithProjectCapabilitiesAndSnapshot() {
        when(standardSnapshotService.getCurrentSnapshot(7L)).thenReturn(new StandardSnapshotInfo(
                11L,
                7L,
                "v2026.07.04",
                "当前标准",
                "供 AI 使用",
                "abc123",
                LocalDateTime.of(2026, 7, 4, 10, 0),
                true
        ));

        AiSessionBootstrap bootstrap = service.getBootstrap(7L, "http://localhost:8090/", true);

        assertEquals("dataspec-ai-session-bootstrap", bootstrap.kind());
        assertEquals(1, bootstrap.schemaVersion());
        assertEquals("READY", bootstrap.status());
        assertEquals(7L, bootstrap.projectId());
        assertEquals("http://localhost:8090", bootstrap.server());
        assertEquals("TOKEN_PRESENT", bootstrap.authMode());
        assertEquals("v2026.07.04", bootstrap.specVersion());
        assertEquals("abc123", bootstrap.standardSnapshot().specHash());
        assertTrue(bootstrap.availableCapabilities().stream().anyMatch(capability -> "lint-sql".equals(capability.id())));
        assertTrue(bootstrap.availableCapabilities().stream().anyMatch(capability -> "reverse-import".equals(capability.id())));
        assertTrue(bootstrap.recommendedCommands().stream().anyMatch(command -> command.contains("dataspec lint")));
        assertTrue(bootstrap.recommendedCommands().stream().anyMatch(command -> command.contains("export-context")));
        assertTrue(bootstrap.recommendedCommands().stream().anyMatch(command -> command.contains("reverse-import-standards")));
        assertTrue(bootstrap.recommendedCommands().stream().anyMatch(command -> command.contains("generate-ddl")));
        assertTrue(bootstrap.docsRefs().contains("README.md#ai-会话启动包"));
        assertTrue(bootstrap.checks().stream().allMatch(check -> !"fail".equalsIgnoreCase(check.status())));
    }

    @Test
    void missingProjectReturnsBlockedPackageWithProjectSelectionAction() {
        AiSessionBootstrap bootstrap = service.getBootstrap(null, "http://localhost:8090", false);

        assertEquals("BLOCKED", bootstrap.status());
        assertEquals("TOKEN_MISSING", bootstrap.authMode());
        assertEquals("unselected", bootstrap.specVersion());
        assertTrue(bootstrap.checks().stream().anyMatch(check -> "project".equals(check.name()) && "fail".equals(check.status())));
        assertTrue(bootstrap.nextActions().stream().anyMatch(action -> "SELECT_PROJECT".equals(action.code())));
        verify(standardSnapshotService, never()).getCurrentSnapshot(null);
    }

    @Test
    void unversionedStandardDowngradesStatusAndSuggestsSnapshotRefresh() {
        when(standardSnapshotService.getCurrentSnapshot(7L)).thenReturn(StandardSnapshotInfo.unversioned(7L));

        AiSessionBootstrap bootstrap = service.getBootstrap(7L, "http://localhost:8090", false);

        assertEquals("DEGRADED", bootstrap.status());
        assertEquals("unversioned", bootstrap.specVersion());
        assertFalse(bootstrap.standardSnapshot().versioned());
        assertTrue(bootstrap.checks().stream().anyMatch(check -> "standard".equals(check.name()) && "warn".equals(check.status())));
        assertTrue(bootstrap.nextActions().stream().anyMatch(action -> "REFRESH_STANDARD_SNAPSHOT".equals(action.code())));
    }

    @Test
    void bootstrapJsonDoesNotExposeSecrets() throws Exception {
        when(standardSnapshotService.getCurrentSnapshot(7L)).thenReturn(StandardSnapshotInfo.unversioned(7L));

        String json = objectMapper.writeValueAsString(
                service.getBootstrap(7L, "http://user:secret@localhost:8090/path?token=abc", true)
        );

        assertFalse(json.contains("secret"));
        assertFalse(json.contains("Bearer "));
        assertFalse(json.contains("Authorization"));
        assertFalse(json.contains("apiToken"));
        assertFalse(json.contains("jdbc:postgresql://"));
        assertFalse(json.contains("jdbc:mysql://"));
        assertNotNull(json);
    }
}
