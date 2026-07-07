package com.dataspec.changelog.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dataspec.changelog.entity.StandardChangeLog;
import com.dataspec.changelog.mapper.StandardChangeLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 标准变更记录 Repository。
 */
@Repository
@RequiredArgsConstructor
public class StandardChangeLogRepository {

    private final StandardChangeLogMapper standardChangeLogMapper;

    public IPage<StandardChangeLog> page(Long projectId, String targetType, Long targetId, int current, int size) {
        LambdaQueryWrapper<StandardChangeLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StandardChangeLog::getProjectId, projectId);
        if (targetType != null && !targetType.isBlank()) {
            wrapper.eq(StandardChangeLog::getTargetType, targetType);
        }
        if (targetId != null) {
            wrapper.eq(StandardChangeLog::getTargetId, targetId);
        }
        wrapper.orderByDesc(StandardChangeLog::getChangedAt).orderByDesc(StandardChangeLog::getId);
        return standardChangeLogMapper.selectPage(new Page<>(current, size), wrapper);
    }

    public int insert(StandardChangeLog log) {
        return standardChangeLogMapper.insert(log);
    }

    public Optional<StandardChangeLog> findById(Long id) {
        return Optional.ofNullable(standardChangeLogMapper.selectById(id));
    }

    public List<StandardChangeLog> findByProjectId(Long projectId, int limit) {
        return standardChangeLogMapper.selectList(new LambdaQueryWrapper<StandardChangeLog>()
                .eq(StandardChangeLog::getProjectId, projectId)
                .orderByDesc(StandardChangeLog::getChangedAt)
                .orderByDesc(StandardChangeLog::getId)
                .last("limit " + Math.max(1, limit)));
    }

    /**
     * 查询目标对象的变更日志安全摘要列，避免把 beforeJson/afterJson 原始快照带入证据响应。
     */
    public List<StandardChangeLog> findSummaryByTarget(Long projectId, String targetType, Long targetId, int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 100));
        return standardChangeLogMapper.selectList(new LambdaQueryWrapper<StandardChangeLog>()
                .select(
                        StandardChangeLog::getId,
                        StandardChangeLog::getProjectId,
                        StandardChangeLog::getTargetType,
                        StandardChangeLog::getTargetId,
                        StandardChangeLog::getAction,
                        StandardChangeLog::getOperatorName,
                        StandardChangeLog::getChangedAt)
                .eq(StandardChangeLog::getProjectId, projectId)
                .eq(StandardChangeLog::getTargetType, targetType)
                .eq(StandardChangeLog::getTargetId, targetId)
                .orderByDesc(StandardChangeLog::getChangedAt)
                .orderByDesc(StandardChangeLog::getId)
                .last("limit " + safeLimit));
    }
}
