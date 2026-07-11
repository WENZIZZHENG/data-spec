package com.dataspec.metric.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dataspec.metric.entity.MetricDefinition;
import com.dataspec.metric.mapper.MetricDefinitionMapper;
import com.dataspec.metric.repository.MetricDefinitionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 基于 MyBatis 的指标口径持久化实现。
 */
@Repository
@RequiredArgsConstructor
public class MetricDefinitionRepositoryImpl implements MetricDefinitionRepository {

    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 100;

    private final MetricDefinitionMapper mapper;

    @Override
    public Optional<MetricDefinition> findById(Long id) {
        return Optional.ofNullable(mapper.selectById(id));
    }

    @Override
    public boolean existsByMetricKey(Long projectId, String metricKey, Long excludeId) {
        LambdaQueryWrapper<MetricDefinition> wrapper = new LambdaQueryWrapper<MetricDefinition>()
                .eq(MetricDefinition::getProjectId, projectId)
                .eq(MetricDefinition::getMetricKey, metricKey);
        if (excludeId != null) {
            wrapper.ne(MetricDefinition::getId, excludeId);
        }
        return mapper.exists(wrapper);
    }

    @Override
    public List<MetricDefinition> findByProject(
            Long projectId,
            String query,
            String status,
            Long fieldId,
            String metricKey,
            Integer limit) {
        LambdaQueryWrapper<MetricDefinition> wrapper = new LambdaQueryWrapper<MetricDefinition>()
                .eq(MetricDefinition::getProjectId, projectId)
                .orderByAsc(MetricDefinition::getMetricKey)
                .last("LIMIT " + safeLimit(limit));
        if (status != null && !status.isBlank()) {
            wrapper.eq(MetricDefinition::getStatus, status);
        }
        if (metricKey != null && !metricKey.isBlank()) {
            wrapper.eq(MetricDefinition::getMetricKey, metricKey);
        }
        if (fieldId != null) {
            applyFieldReferenceFilter(wrapper, fieldId);
        }
        if (query != null && !query.isBlank()) {
            String like = query.trim();
            wrapper.and(nested -> nested
                    .like(MetricDefinition::getMetricKey, like)
                    .or()
                    .like(MetricDefinition::getDisplayName, like)
                    .or()
                    .like(MetricDefinition::getDefinition, like)
                    .or()
                    .like(MetricDefinition::getOwnerNotes, like));
        }
        return mapper.selectList(wrapper);
    }

    private void applyFieldReferenceFilter(LambdaQueryWrapper<MetricDefinition> wrapper, Long fieldId) {
        // 字段引用由服务层写入为紧凑 JSON 数组；这里仅拼接内部 Long 值生成 LIKE 模式，避免用户输入进入 SQL 结构。
        applyFieldReferenceFilters(wrapper, List.of(fieldId));
    }

    @Override
    public List<MetricDefinition> findRelatedToFields(Long projectId, List<Long> fieldIds, Integer limit) {
        if (fieldIds == null || fieldIds.isEmpty()) {
            return List.of();
        }
        LambdaQueryWrapper<MetricDefinition> wrapper = new LambdaQueryWrapper<MetricDefinition>()
                .eq(MetricDefinition::getProjectId, projectId)
                .orderByAsc(MetricDefinition::getMetricKey)
                .last("LIMIT " + safeLimit(limit));
        applyFieldReferenceFilters(wrapper, fieldIds);
        return mapper.selectList(wrapper);
    }

    private void applyFieldReferenceFilters(LambdaQueryWrapper<MetricDefinition> wrapper, List<Long> fieldIds) {
        wrapper.and(nested -> {
            boolean firstCondition = true;
            for (Long fieldId : fieldIds) {
                String exact = "[" + fieldId + "]";
                String first = "[" + fieldId + ",";
                String middle = "," + fieldId + ",";
                String last = "," + fieldId + "]";
                if (!firstCondition) {
                    nested.or();
                }
                nested.like(MetricDefinition::getMeasureFieldsJson, exact)
                        .or()
                        .like(MetricDefinition::getMeasureFieldsJson, first)
                        .or()
                        .like(MetricDefinition::getMeasureFieldsJson, middle)
                        .or()
                        .like(MetricDefinition::getMeasureFieldsJson, last)
                        .or()
                        .like(MetricDefinition::getDimensionFieldsJson, exact)
                        .or()
                        .like(MetricDefinition::getDimensionFieldsJson, first)
                        .or()
                        .like(MetricDefinition::getDimensionFieldsJson, middle)
                        .or()
                        .like(MetricDefinition::getDimensionFieldsJson, last);
                firstCondition = false;
            }
        });
    }

    private int safeLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_LIMIT;
        }
        if (limit <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, MAX_LIMIT);
    }

    @Override
    public int insert(MetricDefinition definition) {
        return mapper.insert(definition);
    }

    @Override
    public int update(MetricDefinition definition) {
        return mapper.updateById(definition);
    }

    @Override
    public int deleteById(Long id) {
        return mapper.deleteById(id);
    }
}
