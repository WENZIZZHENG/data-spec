package com.dataspec.standardcandidate.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dataspec.standardcandidate.entity.StandardCandidate;
import com.dataspec.standardcandidate.mapper.StandardCandidateMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 标准候选 Repository。
 */
@Repository
@RequiredArgsConstructor
public class StandardCandidateRepository {

    private static final List<String> ACTIVE_STATUSES = List.of("PENDING", "POSTPONED");

    private final StandardCandidateMapper standardCandidateMapper;

    public Optional<StandardCandidate> findById(Long id) {
        return Optional.ofNullable(standardCandidateMapper.selectById(id));
    }

    public IPage<StandardCandidate> page(Long projectId, String status, String sourceType, String keyword, int current, int size) {
        LambdaQueryWrapper<StandardCandidate> wrapper = new LambdaQueryWrapper<StandardCandidate>()
                .eq(StandardCandidate::getProjectId, projectId)
                .orderByDesc(StandardCandidate::getCreatedAt)
                .orderByDesc(StandardCandidate::getId);
        if (!isBlank(status)) {
            wrapper.eq(StandardCandidate::getStatus, status);
        }
        if (!isBlank(sourceType)) {
            wrapper.eq(StandardCandidate::getSourceType, sourceType);
        }
        if (!isBlank(keyword)) {
            String pattern = keyword.trim();
            wrapper.and(query -> query
                    .like(StandardCandidate::getCandidateName, pattern)
                    .or()
                    .like(StandardCandidate::getDisplayName, pattern)
                    .or()
                    .like(StandardCandidate::getComment, pattern));
        }
        return standardCandidateMapper.selectPage(new Page<>(current, size), wrapper);
    }

    /**
     * 查询项目内候选证据，用于只读聚合字段来源可信度，不暴露 raw evidence 给外部响应。
     */
    public List<StandardCandidate> findByProjectId(Long projectId) {
        return standardCandidateMapper.selectList(
                new LambdaQueryWrapper<StandardCandidate>()
                        .eq(StandardCandidate::getProjectId, projectId)
                        .orderByDesc(StandardCandidate::getCreatedAt)
                        .orderByDesc(StandardCandidate::getId));
    }

    /**
     * 查询候选安全摘要列，供跨来源证据视图聚合使用，避免装载 raw evidenceJson、comment 等可能包含业务样例或凭据的字段。
     */
    public List<StandardCandidate> findSummaryByProjectId(Long projectId) {
        return standardCandidateMapper.selectList(
                new LambdaQueryWrapper<StandardCandidate>()
                        .select(
                                StandardCandidate::getId,
                                StandardCandidate::getProjectId,
                                StandardCandidate::getCandidateName,
                                StandardCandidate::getDisplayName,
                                StandardCandidate::getDataType,
                                StandardCandidate::getSourceType,
                                StandardCandidate::getSourceRef,
                                StandardCandidate::getConfidence,
                                StandardCandidate::getStatus,
                                StandardCandidate::getTargetFieldId,
                                StandardCandidate::getDecisionReason,
                                StandardCandidate::getDecidedAt,
                                StandardCandidate::getCreatedAt,
                                StandardCandidate::getUpdatedAt)
                        .eq(StandardCandidate::getProjectId, projectId)
                        .orderByDesc(StandardCandidate::getDecidedAt)
                        .orderByDesc(StandardCandidate::getCreatedAt)
                        .orderByDesc(StandardCandidate::getId));
    }

    public boolean existsActiveByNameInProject(Long projectId, String candidateName) {
        return standardCandidateMapper.exists(new LambdaQueryWrapper<StandardCandidate>()
                .eq(StandardCandidate::getProjectId, projectId)
                .eq(StandardCandidate::getCandidateName, candidateName)
                .in(StandardCandidate::getStatus, ACTIVE_STATUSES));
    }

    public int countByStatuses(Long projectId, List<String> statuses) {
        if (statuses == null || statuses.isEmpty()) {
            return 0;
        }
        Long count = standardCandidateMapper.selectCount(new LambdaQueryWrapper<StandardCandidate>()
                .eq(StandardCandidate::getProjectId, projectId)
                .in(StandardCandidate::getStatus, statuses));
        return Math.toIntExact(count == null ? 0L : count);
    }

    public int insert(StandardCandidate candidate) {
        return standardCandidateMapper.insert(candidate);
    }

    public int update(StandardCandidate candidate) {
        return standardCandidateMapper.updateById(candidate);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
