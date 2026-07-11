package com.dataspec.metric.repository;

import com.dataspec.metric.entity.MetricDefinition;

import java.util.List;
import java.util.Optional;

/**
 * 指标口径持久化端口，封装项目、指标键、字段引用和状态维度的查询。
 */
public interface MetricDefinitionRepository {

    Optional<MetricDefinition> findById(Long id);

    boolean existsByMetricKey(Long projectId, String metricKey, Long excludeId);

    default List<MetricDefinition> findByProject(Long projectId, String query, String status) {
        return findByProject(projectId, query, status, null, null, null);
    }

    /**
     * 按项目查询指标口径，并在持久化层下推字段引用、metricKey 和 limit 边界。
     */
    List<MetricDefinition> findByProject(
            Long projectId,
            String query,
            String status,
            Long fieldId,
            String metricKey,
            Integer limit);

    /**
     * 按项目和字段集合查询引用任一字段的指标口径，并在持久化层应用上限。
     */
    List<MetricDefinition> findRelatedToFields(Long projectId, List<Long> fieldIds, Integer limit);

    int insert(MetricDefinition definition);

    int update(MetricDefinition definition);

    int deleteById(Long id);
}
