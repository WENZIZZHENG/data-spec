package com.dataspec.standardqualitygate.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dataspec.standardqualitygate.entity.StandardQualityGate;
import com.dataspec.standardqualitygate.mapper.StandardQualityGateMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 标准质量门禁配置 Repository。
 */
@Repository
@RequiredArgsConstructor
public class StandardQualityGateRepository {

    private final StandardQualityGateMapper standardQualityGateMapper;

    public Optional<StandardQualityGate> findByProjectId(Long projectId) {
        return standardQualityGateMapper.selectList(new LambdaQueryWrapper<StandardQualityGate>()
                        .eq(StandardQualityGate::getProjectId, projectId)
                        .last("LIMIT 1"))
                .stream()
                .findFirst();
    }

    public int insert(StandardQualityGate gate) {
        return standardQualityGateMapper.insert(gate);
    }

    public int update(StandardQualityGate gate) {
        return standardQualityGateMapper.updateById(gate);
    }
}
