package com.dataspec.tablemodel.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dataspec.tablemodel.entity.BusinessObjectStandard;
import com.dataspec.tablemodel.mapper.BusinessObjectStandardMapper;
import com.dataspec.tablemodel.repository.BusinessObjectStandardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 基于 MyBatis-Plus 的业务对象标准持久化实现。
 */
@Repository
@RequiredArgsConstructor
public class BusinessObjectStandardRepositoryImpl implements BusinessObjectStandardRepository {

    private final BusinessObjectStandardMapper mapper;

    @Override
    public List<BusinessObjectStandard> findByProjectId(Long projectId) {
        return mapper.selectList(new LambdaQueryWrapper<BusinessObjectStandard>()
                .eq(BusinessObjectStandard::getProjectId, projectId)
                .orderByAsc(BusinessObjectStandard::getObjectKey));
    }

    @Override
    public Optional<BusinessObjectStandard> findById(Long id) {
        return Optional.ofNullable(mapper.selectById(id));
    }

    @Override
    public Optional<BusinessObjectStandard> findByObjectKey(Long projectId, String objectKey) {
        return Optional.ofNullable(mapper.selectOne(new LambdaQueryWrapper<BusinessObjectStandard>()
                .eq(BusinessObjectStandard::getProjectId, projectId)
                .eq(BusinessObjectStandard::getObjectKey, objectKey)
                .last("limit 1")));
    }

    @Override
    public List<BusinessObjectStandard> findByTemplateId(Long templateId) {
        return mapper.selectList(new LambdaQueryWrapper<BusinessObjectStandard>()
                .eq(BusinessObjectStandard::getTemplateId, templateId)
                .orderByAsc(BusinessObjectStandard::getObjectKey));
    }

    @Override
    public boolean existsByObjectKey(Long projectId, String objectKey, Long excludeId) {
        LambdaQueryWrapper<BusinessObjectStandard> query = new LambdaQueryWrapper<BusinessObjectStandard>()
                .eq(BusinessObjectStandard::getProjectId, projectId)
                .eq(BusinessObjectStandard::getObjectKey, objectKey);
        if (excludeId != null) {
            query.ne(BusinessObjectStandard::getId, excludeId);
        }
        return mapper.exists(query);
    }

    @Override
    public boolean existsByEntityName(Long projectId, String entityName, Long excludeId) {
        LambdaQueryWrapper<BusinessObjectStandard> query = new LambdaQueryWrapper<BusinessObjectStandard>()
                .eq(BusinessObjectStandard::getProjectId, projectId)
                .eq(BusinessObjectStandard::getEntityName, entityName);
        if (excludeId != null) {
            query.ne(BusinessObjectStandard::getId, excludeId);
        }
        return mapper.exists(query);
    }

    @Override
    public int insert(BusinessObjectStandard standard) {
        return mapper.insert(standard);
    }

    @Override
    public int update(BusinessObjectStandard standard) {
        return mapper.updateById(standard);
    }

    @Override
    public int deleteById(Long id) {
        return mapper.deleteById(id);
    }
}
