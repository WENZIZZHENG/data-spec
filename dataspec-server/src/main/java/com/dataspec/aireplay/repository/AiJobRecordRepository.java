package com.dataspec.aireplay.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dataspec.aireplay.entity.AiJobRecord;
import com.dataspec.aireplay.mapper.AiJobRecordMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * AI 作业回放记录 Repository。
 */
@Repository
@RequiredArgsConstructor
public class AiJobRecordRepository {

    private final AiJobRecordMapper aiJobRecordMapper;

    public int insert(AiJobRecord record) {
        return aiJobRecordMapper.insert(record);
    }

    public Optional<AiJobRecord> findById(Long id) {
        return Optional.ofNullable(aiJobRecordMapper.selectById(id));
    }

    public List<AiJobRecord> findRecentByProjectId(Long projectId, int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 500));
        return aiJobRecordMapper.selectList(
                new LambdaQueryWrapper<AiJobRecord>()
                        .eq(AiJobRecord::getProjectId, projectId)
                        .orderByDesc(AiJobRecord::getCreatedAt)
                        .orderByDesc(AiJobRecord::getId)
                        .last("LIMIT " + safeLimit));
    }

    /**
     * 查询热区报告所需的近期 AI 作业摘要列，不装载原始输入输出 payload，避免敏感上下文进入报告聚合。
     */
    public List<AiJobRecord> findRecentSummaryByProjectId(Long projectId, int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 500));
        return aiJobRecordMapper.selectList(
                new LambdaQueryWrapper<AiJobRecord>()
                        .select(
                                AiJobRecord::getId,
                                AiJobRecord::getProjectId,
                                AiJobRecord::getJobType,
                                AiJobRecord::getTitle,
                                AiJobRecord::getInputSummary,
                                AiJobRecord::getPromptVersion,
                                AiJobRecord::getStatus,
                                AiJobRecord::getSqlCheckRecordId,
                                AiJobRecord::getCreatedAt,
                                AiJobRecord::getUpdatedAt)
                        .eq(AiJobRecord::getProjectId, projectId)
                        .orderByDesc(AiJobRecord::getCreatedAt)
                        .orderByDesc(AiJobRecord::getId)
                        .last("LIMIT " + safeLimit));
    }

    public IPage<AiJobRecord> findByProjectId(Long projectId, String jobType, int current, int size) {
        LambdaQueryWrapper<AiJobRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.select(
                AiJobRecord::getId,
                AiJobRecord::getProjectId,
                AiJobRecord::getJobType,
                AiJobRecord::getTitle,
                AiJobRecord::getInputSummary,
                AiJobRecord::getPromptVersion,
                AiJobRecord::getStatus,
                AiJobRecord::getSqlCheckRecordId,
                AiJobRecord::getStandardSnapshotId,
                AiJobRecord::getStandardSnapshotVersion,
                AiJobRecord::getStandardSnapshotHash,
                AiJobRecord::getCreatedAt,
                AiJobRecord::getUpdatedAt
        );
        wrapper.eq(AiJobRecord::getProjectId, projectId);
        if (jobType != null && !jobType.isBlank()) {
            wrapper.eq(AiJobRecord::getJobType, jobType.trim());
        }
        wrapper.orderByDesc(AiJobRecord::getCreatedAt).orderByDesc(AiJobRecord::getId);
        return aiJobRecordMapper.selectPage(new Page<>(current, size), wrapper);
    }
}
