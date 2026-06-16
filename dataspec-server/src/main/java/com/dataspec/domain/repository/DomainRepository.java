package com.dataspec.domain.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dataspec.domain.entity.Domain;
import com.dataspec.domain.mapper.DomainMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class DomainRepository {

    private final DomainMapper domainMapper;

    public Optional<Domain> findById(Long id) {
        return Optional.ofNullable(domainMapper.selectById(id));
    }

    public List<Domain> findByProjectId(Long projectId) {
        return domainMapper.selectList(
                new LambdaQueryWrapper<Domain>()
                        .eq(Domain::getProjectId, projectId)
                        .orderByAsc(Domain::getCode));
    }

    public boolean existsByCodeInProject(String code, Long projectId) {
        return domainMapper.exists(
                new LambdaQueryWrapper<Domain>()
                        .eq(Domain::getCode, code)
                        .eq(Domain::getProjectId, projectId));
    }

    public boolean existsByCodeInProjectExcludeId(String code, Long projectId, Long excludeId) {
        return domainMapper.exists(
                new LambdaQueryWrapper<Domain>()
                        .eq(Domain::getCode, code)
                        .eq(Domain::getProjectId, projectId)
                        .ne(Domain::getId, excludeId));
    }

    public int insert(Domain domain) {
        return domainMapper.insert(domain);
    }

    public int update(Domain domain) {
        return domainMapper.updateById(domain);
    }

    public int deleteById(Long id) {
        return domainMapper.deleteById(id);
    }
}
