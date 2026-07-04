package com.dataspec.aitaskrun.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dataspec.aitaskrun.entity.AiTaskRun;
import com.dataspec.aitaskrun.mapper.AiTaskRunMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * AI 任务运行记录 Repository。
 */
@Repository
@RequiredArgsConstructor
public class AiTaskRunRepository {

    private final AiTaskRunMapper aiTaskRunMapper;

    public int insert(AiTaskRun run) {
        return aiTaskRunMapper.insert(run);
    }

    public int update(AiTaskRun run) {
        return aiTaskRunMapper.updateById(run);
    }

    public Optional<AiTaskRun> findById(Long id) {
        return Optional.ofNullable(aiTaskRunMapper.selectById(id));
    }

    public IPage<AiTaskRun> findByProjectId(Long projectId,
                                            String status,
                                            String taskType,
                                            int current,
                                            int size) {
        LambdaQueryWrapper<AiTaskRun> wrapper = listWrapper();
        wrapper.eq(AiTaskRun::getProjectId, projectId);
        if (status != null && !status.isBlank()) {
            wrapper.eq(AiTaskRun::getStatus, status);
        }
        if (taskType != null && !taskType.isBlank()) {
            wrapper.eq(AiTaskRun::getTaskType, taskType);
        }
        wrapper.orderByDesc(AiTaskRun::getCreatedAt).orderByDesc(AiTaskRun::getId);
        return aiTaskRunMapper.selectPage(new Page<>(current, size), wrapper);
    }

    public List<AiTaskRun> findRecentFailures(Long projectId, int limit) {
        LambdaQueryWrapper<AiTaskRun> wrapper = listWrapper();
        wrapper.eq(AiTaskRun::getProjectId, projectId);
        wrapper.in(AiTaskRun::getStatus, List.of("FAILED", "PARTIAL_FAILED"));
        wrapper.orderByDesc(AiTaskRun::getCreatedAt).orderByDesc(AiTaskRun::getId);
        wrapper.last("LIMIT " + Math.max(1, Math.min(limit, 100)));
        return aiTaskRunMapper.selectList(wrapper);
    }

    private LambdaQueryWrapper<AiTaskRun> listWrapper() {
        LambdaQueryWrapper<AiTaskRun> wrapper = new LambdaQueryWrapper<>();
        wrapper.select(
                AiTaskRun::getId,
                AiTaskRun::getProjectId,
                AiTaskRun::getTaskType,
                AiTaskRun::getSourceType,
                AiTaskRun::getSourceId,
                AiTaskRun::getStatus,
                AiTaskRun::getInputHash,
                AiTaskRun::getRetryable,
                AiTaskRun::getFailedStep,
                AiTaskRun::getResumeCommand,
                AiTaskRun::getNextAction,
                AiTaskRun::getOperatorName,
                AiTaskRun::getStartedAt,
                AiTaskRun::getFinishedAt,
                AiTaskRun::getExpiresAt,
                AiTaskRun::getCreatedAt,
                AiTaskRun::getUpdatedAt
        );
        return wrapper;
    }
}
