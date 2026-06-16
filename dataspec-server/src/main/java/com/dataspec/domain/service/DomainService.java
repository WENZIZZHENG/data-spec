package com.dataspec.domain.service;

import com.dataspec.domain.entity.Domain;

import java.util.List;

public interface DomainService {
    List<Domain> listByProject(Long projectId);
    Domain getById(Long id);
    Domain create(Domain domain);
    Domain update(Long id, Domain domain);
    void delete(Long id);
}
