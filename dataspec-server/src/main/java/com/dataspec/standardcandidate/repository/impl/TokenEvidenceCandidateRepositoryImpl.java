package com.dataspec.standardcandidate.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dataspec.standardcandidate.entity.StandardCandidate;
import com.dataspec.standardcandidate.mapper.StandardCandidateMapper;
import com.dataspec.standardcandidate.repository.TokenEvidenceCandidateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/** MyBatis 命名证据候选持久化实现。 */
@Repository
@RequiredArgsConstructor
public class TokenEvidenceCandidateRepositoryImpl implements TokenEvidenceCandidateRepository {

    private static final List<String> ACTIVE_STATUSES = List.of("PENDING", "POSTPONED");

    private final StandardCandidateMapper standardCandidateMapper;

    @Override
    public Optional<StandardCandidate> findByFactKey(
            Long projectId,
            String candidateName,
            String sourceType,
            String sourceRef
    ) {
        return standardCandidateMapper.selectList(new LambdaQueryWrapper<StandardCandidate>()
                        .eq(StandardCandidate::getProjectId, projectId)
                        .eq(StandardCandidate::getCandidateName, candidateName)
                        .eq(StandardCandidate::getSourceType, sourceType)
                        .eq(StandardCandidate::getSourceRef, sourceRef)
                        .orderByDesc(StandardCandidate::getId)
                        .last("LIMIT 1"))
                .stream()
                .findFirst();
    }

    @Override
    public Optional<StandardCandidate> findActiveByName(Long projectId, String candidateName) {
        return standardCandidateMapper.selectList(new LambdaQueryWrapper<StandardCandidate>()
                        .eq(StandardCandidate::getProjectId, projectId)
                        .eq(StandardCandidate::getCandidateName, candidateName)
                        .in(StandardCandidate::getStatus, ACTIVE_STATUSES)
                        .orderByDesc(StandardCandidate::getCreatedAt)
                        .orderByDesc(StandardCandidate::getId)
                        .last("LIMIT 1"))
                .stream()
                .findFirst();
    }

    @Override
    public int insertIfAbsent(StandardCandidate candidate) {
        return standardCandidateMapper.insertTokenEvidenceIfAbsent(candidate);
    }
}
