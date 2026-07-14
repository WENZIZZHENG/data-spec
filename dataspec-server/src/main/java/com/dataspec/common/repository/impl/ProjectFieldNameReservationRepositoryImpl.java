package com.dataspec.common.repository.impl;

import com.dataspec.common.mapper.ProjectFieldNameReservationMapper;
import com.dataspec.common.repository.ProjectFieldNameReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * 基于 PostgreSQL 事务级 advisory lock 的项目字段名预留实现。
 */
@Repository
@RequiredArgsConstructor
public class ProjectFieldNameReservationRepositoryImpl implements ProjectFieldNameReservationRepository {

    private final ProjectFieldNameReservationMapper mapper;

    @Override
    public void lock(Long projectId, String fieldName) {
        mapper.lock(projectId, fieldName);
    }

    @Override
    public boolean existsActiveCandidate(Long projectId, String fieldName, Long excludedCandidateId) {
        if (excludedCandidateId == null) {
            return mapper.existsActiveCandidate(projectId, fieldName);
        }
        return mapper.existsOtherActiveCandidate(projectId, fieldName, excludedCandidateId);
    }
}
