package com.dataspec.starterkit.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dataspec.starterkit.entity.StarterKitInstallation;
import com.dataspec.starterkit.mapper.StarterKitInstallationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Starter Kit 安装记录 Repository。
 */
@Repository
@RequiredArgsConstructor
public class StarterKitInstallationRepository {

    private final StarterKitInstallationMapper mapper;

    public List<StarterKitInstallation> findByProjectId(Long projectId) {
        return mapper.selectList(
                new LambdaQueryWrapper<StarterKitInstallation>()
                        .eq(StarterKitInstallation::getProjectId, projectId)
                        .orderByDesc(StarterKitInstallation::getAppliedAt)
                        .orderByDesc(StarterKitInstallation::getId));
    }

    public int insert(StarterKitInstallation installation) {
        return mapper.insert(installation);
    }
}
