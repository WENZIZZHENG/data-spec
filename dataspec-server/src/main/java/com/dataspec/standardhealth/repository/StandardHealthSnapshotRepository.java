package com.dataspec.standardhealth.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dataspec.standardhealth.entity.StandardHealthSnapshot;
import com.dataspec.standardhealth.mapper.StandardHealthSnapshotMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 标准健康快照 Repository。
 */
@Repository
@RequiredArgsConstructor
public class StandardHealthSnapshotRepository {

    private final StandardHealthSnapshotMapper standardHealthSnapshotMapper;

    public int insert(StandardHealthSnapshot snapshot) {
        return standardHealthSnapshotMapper.insert(snapshot);
    }

    public Optional<StandardHealthSnapshot> findLatestByProjectId(Long projectId) {
        return standardHealthSnapshotMapper.selectList(baseQuery(projectId).last("LIMIT 1"))
                .stream()
                .findFirst();
    }

    public List<StandardHealthSnapshot> findRecentByProjectId(Long projectId, int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 120));
        return standardHealthSnapshotMapper.selectList(baseQuery(projectId).last("LIMIT " + safeLimit));
    }

    private LambdaQueryWrapper<StandardHealthSnapshot> baseQuery(Long projectId) {
        return new LambdaQueryWrapper<StandardHealthSnapshot>()
                .eq(StandardHealthSnapshot::getProjectId, projectId)
                .orderByDesc(StandardHealthSnapshot::getCapturedAt)
                .orderByDesc(StandardHealthSnapshot::getId);
    }
}
