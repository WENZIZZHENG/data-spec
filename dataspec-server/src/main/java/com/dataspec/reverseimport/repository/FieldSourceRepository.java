package com.dataspec.reverseimport.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dataspec.reverseimport.entity.FieldSource;
import com.dataspec.reverseimport.mapper.FieldSourceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Objects;

/**
 * 标准字段来源 Repository。
 */
@Repository
@RequiredArgsConstructor
public class FieldSourceRepository {

    private final FieldSourceMapper fieldSourceMapper;

    public int insert(FieldSource source) {
        return fieldSourceMapper.insert(source);
    }

    public List<FieldSource> findByFieldId(Long fieldId) {
        return fieldSourceMapper.selectList(
                new LambdaQueryWrapper<FieldSource>()
                        .eq(FieldSource::getFieldId, fieldId)
                        .orderByDesc(FieldSource::getCreatedAt)
                        .orderByDesc(FieldSource::getId));
    }

    public List<FieldSource> findByProjectId(Long projectId) {
        return fieldSourceMapper.selectList(
                new LambdaQueryWrapper<FieldSource>()
                        .eq(FieldSource::getProjectId, projectId)
                        .orderByDesc(FieldSource::getCreatedAt)
                        .orderByDesc(FieldSource::getId));
    }

    /**
     * 查询热区报告所需的字段来源摘要列，避免把 raw metadata、默认值或库表字段注释带入只读聚合链路。
     */
    public List<FieldSource> findSummaryByProjectId(Long projectId) {
        return fieldSourceMapper.selectList(
                new LambdaQueryWrapper<FieldSource>()
                        .select(
                                FieldSource::getId,
                                FieldSource::getProjectId,
                                FieldSource::getFieldId,
                                FieldSource::getSourceType,
                                FieldSource::getCreatedAt)
                        .eq(FieldSource::getProjectId, projectId)
                        .orderByDesc(FieldSource::getCreatedAt)
                        .orderByDesc(FieldSource::getId));
    }

    public List<Long> findFieldIdsByProjectAndBatch(Long projectId, Long batchId) {
        return fieldSourceMapper.selectList(
                        new LambdaQueryWrapper<FieldSource>()
                                .eq(FieldSource::getProjectId, projectId)
                                .eq(FieldSource::getBatchId, batchId))
                .stream()
                .map(FieldSource::getFieldId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }
}
