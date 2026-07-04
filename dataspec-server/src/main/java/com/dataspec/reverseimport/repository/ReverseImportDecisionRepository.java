package com.dataspec.reverseimport.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dataspec.reverseimport.entity.ReverseImportDecision;
import com.dataspec.reverseimport.mapper.ReverseImportDecisionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 数据库反向导入字段映射决策 Repository。
 */
@Repository
@RequiredArgsConstructor
public class ReverseImportDecisionRepository {

    private final ReverseImportDecisionMapper reverseImportDecisionMapper;

    public int insert(ReverseImportDecision decision) {
        return reverseImportDecisionMapper.insert(decision);
    }

    public List<ReverseImportDecision> findByBatchId(Long batchId) {
        return reverseImportDecisionMapper.selectList(
                new LambdaQueryWrapper<ReverseImportDecision>()
                        .eq(ReverseImportDecision::getBatchId, batchId)
                        .orderByDesc(ReverseImportDecision::getCreatedAt)
                        .orderByDesc(ReverseImportDecision::getId));
    }

    public List<ReverseImportDecision> findRecentByProjectId(Long projectId, int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 500));
        return reverseImportDecisionMapper.selectList(
                new LambdaQueryWrapper<ReverseImportDecision>()
                        .eq(ReverseImportDecision::getProjectId, projectId)
                        .orderByDesc(ReverseImportDecision::getCreatedAt)
                        .orderByDesc(ReverseImportDecision::getId)
                        .last("LIMIT " + safeLimit));
    }
}
