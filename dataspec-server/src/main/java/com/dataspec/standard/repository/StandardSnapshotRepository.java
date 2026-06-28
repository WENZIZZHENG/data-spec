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

    public Optional<StandardSnapshot> findByProjectIdAndId(Long projectId, Long snapshotId) {
        return Optional.ofNullable(standardSnapshotMapper.selectOne(new LambdaQueryWrapper<StandardSnapshot>()
                .eq(StandardSnapshot::getProjectId, projectId)
                .eq(StandardSnapshot::getId, snapshotId)
                .last("limit 1")));
    }

    public Optional<StandardSnapshot> findByProjectIdAndVersion(Long projectId, String version) {
        return Optional.ofNullable(standardSnapshotMapper.selectOne(new LambdaQueryWrapper<StandardSnapshot>()
                .eq(StandardSnapshot::getProjectId, projectId)
                .eq(StandardSnapshot::getVersion, version)
                .last("limit 1")));
    }

    public List<StandardSnapshot> findByProjectId(Long projectId) {
        return standardSnapshotMapper.selectList(new LambdaQueryWrapper<StandardSnapshot>()
                .eq(StandardSnapshot::getProjectId, projectId)
                .orderByDesc(StandardSnapshot::getCreatedAt)
                .orderByDesc(StandardSnapshot::getId));
    }

    public List<StandardSnapshot> findRecentByProjectId(Long projectId, int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 500));
        return standardSnapshotMapper.selectList(new LambdaQueryWrapper<StandardSnapshot>()
                .eq(StandardSnapshot::getProjectId, projectId)
                .orderByDesc(StandardSnapshot::getCreatedAt)
                .orderByDesc(StandardSnapshot::getId)
                .last("LIMIT " + safeLimit));
    }
}
