package com.dataspec.dbpreset.service.impl;

import com.dataspec.common.exception.BizException;
import com.dataspec.dbpreset.entity.DatabaseConnectionPreset;
import com.dataspec.dbpreset.repository.DatabaseConnectionPresetRepository;
import com.dataspec.dbpreset.service.DatabaseConnectionPresetService;
import com.dataspec.security.context.ProjectAccessGuard;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;

/**
 * 数据库连接预设服务实现。
 */
@Service
@RequiredArgsConstructor
public class DatabaseConnectionPresetServiceImpl implements DatabaseConnectionPresetService {

    private final DatabaseConnectionPresetRepository repository;
    private final ObjectMapper objectMapper;

    @Override
    public List<DatabaseConnectionPreset> listByProject(Long projectId) {
        if (projectId == null) {
            throw new BizException("项目ID不能为空");
        }
        ProjectAccessGuard.requireProjectAccess(projectId);
        return repository.findByProjectId(projectId).stream()
                .map(this::attachTableNames)
                .toList();
    }

    @Override
    public DatabaseConnectionPreset getById(Long id) {
        DatabaseConnectionPreset preset = repository.findById(id)
                .orElseThrow(() -> new BizException("数据库连接预设不存在: " + id));
        ProjectAccessGuard.requireProjectAccess(preset.getProjectId());
        return attachTableNames(preset);
    }

    @Override
    public DatabaseConnectionPreset create(DatabaseConnectionPreset preset) {
        normalizeAndValidate(preset);
        ProjectAccessGuard.requireProjectAccess(preset.getProjectId());
        preset.setTableNamesJson(writeTableNames(preset.getTableNames()));
        repository.insert(preset);
        return attachTableNames(preset);
    }

    @Override
    public DatabaseConnectionPreset update(Long id, DatabaseConnectionPreset preset) {
        DatabaseConnectionPreset existing = getById(id);
        existing.setName(preset.getName());
        existing.setDatabaseType(preset.getDatabaseType());
        existing.setHost(preset.getHost());
        existing.setPort(preset.getPort());
        existing.setDatabaseName(preset.getDatabaseName());
        existing.setSchemaName(preset.getSchemaName());
        existing.setTableNames(preset.getTableNames());
        normalizeAndValidate(existing);
        existing.setTableNamesJson(writeTableNames(existing.getTableNames()));
        repository.update(existing);
        return attachTableNames(existing);
    }

    @Override
    public void delete(Long id) {
        DatabaseConnectionPreset existing = getById(id);
        repository.deleteById(existing.getId());
    }

    private void normalizeAndValidate(DatabaseConnectionPreset preset) {
        if (preset == null) {
            throw new BizException("数据库连接预设不能为空");
        }
        if (preset.getProjectId() == null) {
            throw new BizException("项目ID不能为空");
        }
        preset.setName(requiredText(preset.getName(), "预设名称不能为空"));
        preset.setDatabaseType(requiredText(preset.getDatabaseType(), "数据库类型不能为空"));
        preset.setHost(requiredText(preset.getHost(), "主机不能为空"));
        preset.setDatabaseName(requiredText(preset.getDatabaseName(), "数据库名不能为空"));
        preset.setSchemaName(trimToNull(preset.getSchemaName()));
        if (preset.getPort() == null || preset.getPort() < 1 || preset.getPort() > 65535) {
            throw new BizException("端口必须在 1-65535 之间");
        }
        preset.setTableNames(normalizeTableNames(preset.getTableNames()));
    }

    private String requiredText(String value, String message) {
        String text = trimToNull(value);
        if (text == null) {
            throw new BizException(message);
        }
        return text;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private List<String> normalizeTableNames(List<String> tableNames) {
        if (tableNames == null || tableNames.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<String> names = new LinkedHashSet<>();
        for (String tableName : tableNames) {
            String normalized = trimToNull(tableName);
            if (normalized != null) {
                names.add(normalized);
            }
        }
        return List.copyOf(names);
    }

    private String writeTableNames(List<String> tableNames) {
        List<String> names = normalizeTableNames(tableNames);
        if (names.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(names);
        } catch (Exception e) {
            throw new BizException("表选择序列化失败");
        }
    }

    private DatabaseConnectionPreset attachTableNames(DatabaseConnectionPreset preset) {
        preset.setTableNames(readTableNames(preset.getTableNamesJson()));
        return preset;
    }

    private List<String> readTableNames(String tableNamesJson) {
        if (tableNamesJson == null || tableNamesJson.isBlank()) {
            return List.of();
        }
        try {
            return normalizeTableNames(objectMapper.readValue(tableNamesJson, new TypeReference<>() {}));
        } catch (Exception ignored) {
            return List.of();
        }
    }
}
