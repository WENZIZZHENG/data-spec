package com.dataspec.dbpreset;

import com.dataspec.common.exception.BizException;
import com.dataspec.dbpreset.entity.DatabaseConnectionPreset;
import com.dataspec.dbpreset.repository.DatabaseConnectionPresetRepository;
import com.dataspec.dbpreset.service.impl.DatabaseConnectionPresetServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DatabaseConnectionPresetServiceImplTest {

    @Test
    void create_normalizesAndSerializesNonSensitiveFields() {
        DatabaseConnectionPresetRepository repository = mock(DatabaseConnectionPresetRepository.class);
        DatabaseConnectionPresetServiceImpl service = new DatabaseConnectionPresetServiceImpl(repository, new ObjectMapper());
        DatabaseConnectionPreset preset = basePreset();
        preset.setName(" 本地 PG ");
        preset.setTableNames(List.of(" users ", "", "orders", "users"));

        DatabaseConnectionPreset saved = service.create(preset);

        assertEquals("本地 PG", saved.getName());
        assertEquals(List.of("users", "orders"), saved.getTableNames());
        assertEquals("[\"users\",\"orders\"]", saved.getTableNamesJson());
        verify(repository).insert(saved);
    }

    @Test
    void list_parsesTableNamesAndFallsBackOnBrokenJson() {
        DatabaseConnectionPresetRepository repository = mock(DatabaseConnectionPresetRepository.class);
        DatabaseConnectionPreset first = basePreset();
        first.setTableNamesJson("[\"users\",\"orders\"]");
        DatabaseConnectionPreset broken = basePreset();
        broken.setTableNamesJson("{broken");
        when(repository.findByProjectId(1L)).thenReturn(List.of(first, broken));
        DatabaseConnectionPresetServiceImpl service = new DatabaseConnectionPresetServiceImpl(repository, new ObjectMapper());

        List<DatabaseConnectionPreset> presets = service.listByProject(1L);

        assertEquals(List.of("users", "orders"), presets.get(0).getTableNames());
        assertEquals(List.of(), presets.get(1).getTableNames());
    }

    @Test
    void list_rejectsMissingProjectId() {
        DatabaseConnectionPresetRepository repository = mock(DatabaseConnectionPresetRepository.class);
        DatabaseConnectionPresetServiceImpl service = new DatabaseConnectionPresetServiceImpl(repository, new ObjectMapper());

        BizException error = assertThrows(BizException.class, () -> service.listByProject(null));

        assertTrue(error.getMessage().contains("项目ID"));
        verifyNoInteractions(repository);
    }

    @Test
    void create_rejectsMissingRequiredFields() {
        DatabaseConnectionPresetRepository repository = mock(DatabaseConnectionPresetRepository.class);
        DatabaseConnectionPresetServiceImpl service = new DatabaseConnectionPresetServiceImpl(repository, new ObjectMapper());

        DatabaseConnectionPreset noName = basePreset();
        noName.setName(" ");
        BizException nameError = assertThrows(BizException.class, () -> service.create(noName));
        assertTrue(nameError.getMessage().contains("预设名称"));

        DatabaseConnectionPreset badPort = basePreset();
        badPort.setPort(70000);
        BizException portError = assertThrows(BizException.class, () -> service.create(badPort));
        assertTrue(portError.getMessage().contains("端口"));
    }

    @Test
    void update_keepsOriginalProjectAndWritesTableNames() {
        DatabaseConnectionPresetRepository repository = mock(DatabaseConnectionPresetRepository.class);
        DatabaseConnectionPreset existing = basePreset();
        existing.setId(9L);
        existing.setProjectId(1L);
        when(repository.findById(9L)).thenReturn(Optional.of(existing));
        DatabaseConnectionPresetServiceImpl service = new DatabaseConnectionPresetServiceImpl(repository, new ObjectMapper());
        DatabaseConnectionPreset update = basePreset();
        update.setProjectId(999L);
        update.setName(" 生产库 ");
        update.setHost("10.0.0.8");
        update.setTableNames(List.of("payment_order"));

        DatabaseConnectionPreset saved = service.update(9L, update);

        assertEquals(1L, saved.getProjectId());
        assertEquals("生产库", saved.getName());
        assertEquals("10.0.0.8", saved.getHost());
        assertEquals(List.of("payment_order"), saved.getTableNames());
        verify(repository).update(saved);
    }

    @Test
    void delete_requiresExistingPresetAndDelegates() {
        DatabaseConnectionPresetRepository repository = mock(DatabaseConnectionPresetRepository.class);
        DatabaseConnectionPreset existing = basePreset();
        existing.setId(5L);
        when(repository.findById(5L)).thenReturn(Optional.of(existing));
        DatabaseConnectionPresetServiceImpl service = new DatabaseConnectionPresetServiceImpl(repository, new ObjectMapper());

        service.delete(5L);

        verify(repository).deleteById(5L);
    }

    private DatabaseConnectionPreset basePreset() {
        DatabaseConnectionPreset preset = new DatabaseConnectionPreset();
        preset.setProjectId(1L);
        preset.setName("本地库");
        preset.setDatabaseType("postgresql");
        preset.setHost("localhost");
        preset.setPort(5432);
        preset.setDatabaseName("dataspec_demo");
        preset.setSchemaName("public");
        preset.setTableNames(List.of("users"));
        return preset;
    }
}
