package com.dataspec.dbpreset;

import com.dataspec.dbpreset.controller.DatabaseConnectionPresetController;
import com.dataspec.dbpreset.entity.DatabaseConnectionPreset;
import com.dataspec.dbpreset.service.DatabaseConnectionPresetService;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.*;

class DatabaseConnectionPresetControllerTest {

    @Test
    void listAndGet_delegateToService() {
        DatabaseConnectionPresetService service = mock(DatabaseConnectionPresetService.class);
        DatabaseConnectionPreset preset = new DatabaseConnectionPreset();
        preset.setId(7L);
        when(service.listByProject(1L)).thenReturn(List.of(preset));
        when(service.getById(7L)).thenReturn(preset);
        DatabaseConnectionPresetController controller = new DatabaseConnectionPresetController(service);

        assertSame(preset, controller.list(1L).getData().get(0));
        assertSame(preset, controller.getById(7L).getData());
    }

    @Test
    void createAndUpdate_delegateOnlyNonSensitiveFields() {
        DatabaseConnectionPresetService service = mock(DatabaseConnectionPresetService.class);
        DatabaseConnectionPreset saved = new DatabaseConnectionPreset();
        saved.setId(3L);
        when(service.create(any(DatabaseConnectionPreset.class))).thenReturn(saved);
        when(service.update(eq(3L), any(DatabaseConnectionPreset.class))).thenReturn(saved);
        DatabaseConnectionPresetController controller = new DatabaseConnectionPresetController(service);
        DatabaseConnectionPresetController.PresetReq req = new DatabaseConnectionPresetController.PresetReq(
                1L,
                "本地库",
                "postgresql",
                "localhost",
                5432,
                "dataspec_demo",
                "public",
                List.of("users")
        );

        assertSame(saved, controller.create(req).getData());
        assertSame(saved, controller.update(3L, req).getData());
        verify(service).create(argThat(preset ->
                preset.getProjectId().equals(1L)
                        && "本地库".equals(preset.getName())
                        && "localhost".equals(preset.getHost())
                        && List.of("users").equals(preset.getTableNames())));
        verify(service).update(eq(3L), argThat(preset ->
                preset.getId().equals(3L)
                        && preset.getProjectId().equals(1L)
                        && "postgresql".equals(preset.getDatabaseType())));
    }

    @Test
    void delete_delegatesToService() {
        DatabaseConnectionPresetService service = mock(DatabaseConnectionPresetService.class);
        DatabaseConnectionPresetController controller = new DatabaseConnectionPresetController(service);

        controller.delete(9L);

        verify(service).delete(9L);
    }

    @Test
    void requestSchema_doesNotExposeCredentialsOrConnectionStrings() {
        Set<String> fields = Arrays.stream(DatabaseConnectionPresetController.PresetReq.class.getRecordComponents())
                .map(component -> component.getName().toLowerCase())
                .collect(Collectors.toSet());

        assertFalse(fields.contains("username"));
        assertFalse(fields.contains("password"));
        assertFalse(fields.contains("token"));
        assertFalse(fields.contains("jdbcurl"));
        assertFalse(fields.contains("connectionstring"));
        assertEquals(Set.of(
                "projectid",
                "name",
                "databasetype",
                "host",
                "port",
                "databasename",
                "schemaname",
                "tablenames"
        ), fields);
    }
}
