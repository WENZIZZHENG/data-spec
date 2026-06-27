package com.dataspec.standard.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dataspec.standard.entity.StandardSnapshot;
import com.dataspec.standard.mapper.StandardSnapshotMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 标准版本快照 Repository。
 */
@Repository
@RequiredArgsConstructor
public class StandardSnapshotRepository {

    private final StandardSnapshotMapper standardSnapshotMapper;

    public void save(StandardSnapshot snapshot) {
        standardSnapshotMapper.insert(snapshot);
    }

    public boolean existsByProjectIdAndVersion(Long projectId, String version) {
        return standardSnapshotMapper.exists(new LambdaQueryWrapper<StandardSnapshot>()
                .eq(StandardSnapshot::getProjectId, projectId)
                .eq(StandardSnapshot::getVersion, version));
    }

    public Optional<StandardSnapshot> findLatestByProjectId(Long projectId) {
        return Optional.ofNullable(standardSnapshotMapper.selectOne(new LambdaQueryWrapper<StandardSnapshot>()
                .eq(StandardSnapshot::getProjectId, projectId)
                .orderByDesc(StandardSnapshot::getCreatedAt)
                .orderByDesc(StandardSnapshot::getId)
                .last("limit 1")));
    }

    public List<StandardSnapshot> findByProjectId(Long projectId) {
        return standardSnapshotMapper.selectList(new LambdaQueryWrapper<StandardSnapshot>()
                .eq(StandardSnapshot::getProjectId, projectId)
                .orderByDesc(StandardSnapshot::getCreatedAt)
                .orderByDesc(StandardSnapshot::getId));
    }
}
