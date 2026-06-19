package com.dataspec.domain.service.impl;

import com.dataspec.common.exception.BizException;
import com.dataspec.domain.entity.Domain;
import com.dataspec.domain.repository.DomainRepository;
import com.dataspec.domain.service.DomainService;
import com.dataspec.security.context.ProjectAccessGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 数据域服务实现
 */

@Service
@RequiredArgsConstructor
public class DomainServiceImpl implements DomainService {

    private final DomainRepository domainRepository;

    @Override
    public List<Domain> listByProject(Long projectId) {
        ProjectAccessGuard.requireProjectAccess(projectId);
        return domainRepository.findByProjectId(projectId);
    }

    @Override
    public Domain getById(Long id) {
        Domain domain = domainRepository.findById(id)
                .orElseThrow(() -> new BizException("数据域不存在: " + id));
        ProjectAccessGuard.requireProjectAccess(domain.getProjectId());
        return domain;
    }

    @Override
    public Domain create(Domain domain) {
        ProjectAccessGuard.requireProjectAccess(domain.getProjectId());
        if (domainRepository.existsByCodeInProject(domain.getCode(), domain.getProjectId())) {
            throw new BizException("数据域编码已存在: " + domain.getCode());
        }
        domainRepository.insert(domain);
        return domain;
    }

    @Override
    public Domain update(Long id, Domain domain) {
        Domain existing = getById(id);
        if (domainRepository.existsByCodeInProjectExcludeId(domain.getCode(), existing.getProjectId(), id)) {
            throw new BizException("数据域编码已存在: " + domain.getCode());
        }
        existing.setName(domain.getName());
        existing.setCode(domain.getCode());
        existing.setDescription(domain.getDescription());
        domainRepository.update(existing);
        return existing;
    }

    @Override
    public void delete(Long id) {
        Domain existing = getById(id);
        domainRepository.deleteById(id);
    }
}
