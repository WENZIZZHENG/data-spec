package com.dataspec.dbpreset.service;

import com.dataspec.dbpreset.entity.DatabaseConnectionPreset;

import java.util.List;

/**
 * 数据库连接预设服务。
 */
public interface DatabaseConnectionPresetService {

    List<DatabaseConnectionPreset> listByProject(Long projectId);

    DatabaseConnectionPreset getById(Long id);

    DatabaseConnectionPreset create(DatabaseConnectionPreset preset);

    DatabaseConnectionPreset update(Long id, DatabaseConnectionPreset preset);

    void delete(Long id);
}
