package com.dataspec.reverseimport.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dataspec.reverseimport.entity.ReverseImportBatch;
import com.dataspec.reverseimport.mapper.ReverseImportBatchMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 数据库反向导入批次 Repository。
 */
@Repository
@RequiredArgsConstructor
public class ReverseImportBatchRepository {

    private final ReverseImportBatchMapper reverseImportBatchMapper;

    public int insert(ReverseImportBatch batch) {
        return reverseImportBatchMapper.insert(batch);
    }

    public Optional<ReverseImportBatch> findById(Long id) {
        return Optional.ofNullable(reverseImportBatchMapper.selectById(id));
    }

    public List<ReverseImportBatch> findByProjectId(Long projectId) {
        return reverseImportBatchMapper.selectList(
                new LambdaQueryWrapper<ReverseImportBatch>()
                        .eq(ReverseImportBatch::getProjectId, projectId)
                        .orderByDesc(ReverseImportBatch::getCreatedAt)
                        .orderByDesc(ReverseImportBatch::getId));
    }
}
