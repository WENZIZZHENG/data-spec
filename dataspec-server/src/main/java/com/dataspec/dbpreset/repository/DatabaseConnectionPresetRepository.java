package com.dataspec.dbpreset.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dataspec.dbpreset.entity.DatabaseConnectionPreset;
import com.dataspec.dbpreset.mapper.DatabaseConnectionPresetMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 数据库连接预设 Repository。
 */
@Repository
@RequiredArgsConstructor
public class DatabaseConnectionPresetRepository {

    private final DatabaseConnectionPresetMapper mapper;

    public Optional<DatabaseConnectionPreset> findById(Long id) {
        return Optional.ofNullable(mapper.selectById(id));
    }

    public List<DatabaseConnectionPreset> findByProjectId(Long projectId) {
        return mapper.selectList(
                new LambdaQueryWrapper<DatabaseConnectionPreset>()
                        .eq(DatabaseConnectionPreset::getProjectId, projectId)
                        .orderByDesc(DatabaseConnectionPreset::getUpdatedAt)
                        .orderByDesc(DatabaseConnectionPreset::getId));
    }

    public int insert(DatabaseConnectionPreset preset) {
        return mapper.insert(preset);
    }

    public int update(DatabaseConnectionPreset preset) {
        return mapper.updateById(preset);
    }

    public int deleteById(Long id) {
        return mapper.deleteById(id);
    }
}
