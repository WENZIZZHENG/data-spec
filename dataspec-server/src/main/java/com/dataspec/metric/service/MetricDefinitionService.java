package com.dataspec.metric.service;

import com.dataspec.metric.model.MetricDefinitionReq;
import com.dataspec.metric.model.MetricDefinitionResp;

import java.util.List;

/**
 * 指标口径服务，负责项目边界、字段引用归属和 secret-safe 文本校验。
 */
public interface MetricDefinitionService {

    default List<MetricDefinitionResp> list(Long projectId, String query, String status, Long fieldId) {
        return list(projectId, query, status, fieldId, null, null);
    }

    /**
     * 查询指标口径列表，metricKey 为精确过滤，limit 为可选返回上限，避免导出和工具入口全量拉取。
     */
    List<MetricDefinitionResp> list(
            Long projectId,
            String query,
            String status,
            Long fieldId,
            String metricKey,
            Integer limit);

    /**
     * 查询引用任一字段的指标口径，用于字段知识卡和 scoped AI Context；实现必须保持有界。
     */
    List<MetricDefinitionResp> listRelatedToFields(Long projectId, List<Long> fieldIds, Integer limit);

    MetricDefinitionResp getById(Long id);

    MetricDefinitionResp create(MetricDefinitionReq req);

    MetricDefinitionResp update(Long id, MetricDefinitionReq req);

    void delete(Long id);
}
