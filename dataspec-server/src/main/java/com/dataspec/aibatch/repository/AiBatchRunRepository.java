package com.dataspec.aibatch.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dataspec.aibatch.entity.AiBatchRun;
import com.dataspec.aibatch.mapper.AiBatchRunMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * AI 批量任务运行记录 Repository。
 */
@Repository
@RequiredArgsConstructor
public class AiBatchRunRepository {

    private final AiBatchRunMapper aiBatchRunMapper;

    public int insert(AiBatchRun run) {
        return aiBatchRunMapper.insert(run);
    }

    public int update(AiBatchRun run) {
        return aiBatchRunMapper.updateById(run);
    }

    public Optional<AiBatchRun> findById(Long id) {
        return Optional.ofNullable(aiBatchRunMapper.selectById(id));
    }

    public IPage<AiBatchRun> findByProjectId(Long projectId, int current, int size) {
        LambdaQueryWrapper<AiBatchRun> wrapper = new LambdaQueryWrapper<>();
        wrapper.select(
                AiBatchRun::getId,
                AiBatchRun::getProjectId,
                AiBatchRun::getBatchType,
                AiBatchRun::getSource,
                AiBatchRun::getStatus,
                AiBatchRun::getSummaryJson,
                AiBatchRun::getOperatorName,
                AiBatchRun::getCreatedAt,
                AiBatchRun::getUpdatedAt
        );
        wrapper.eq(AiBatchRun::getProjectId, projectId);
        wrapper.orderByDesc(AiBatchRun::getCreatedAt).orderByDesc(AiBatchRun::getId);
        return aiBatchRunMapper.selectPage(new Page<>(current, size), wrapper);
    }
}
