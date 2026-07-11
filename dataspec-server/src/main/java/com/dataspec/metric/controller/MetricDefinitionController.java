package com.dataspec.metric.controller;

import com.dataspec.common.result.R;
import com.dataspec.metric.model.MetricDefinitionReq;
import com.dataspec.metric.model.MetricDefinitionResp;
import com.dataspec.metric.service.MetricDefinitionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 指标口径 API，维护业务指标到标准字段、过滤条件、聚合方式和时间粒度的轻量映射。
 */
@RestController
@RequestMapping("/api/metric-definitions")
@RequiredArgsConstructor
public class MetricDefinitionController {

    private final MetricDefinitionService service;

    /** 查询指标口径列表。 */
    @GetMapping
    public R<List<MetricDefinitionResp>> list(
            @RequestParam Long projectId,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long fieldId,
            @RequestParam(required = false) String metricKey,
            @RequestParam(required = false) Integer limit) {
        return R.ok(service.list(projectId, query, status, fieldId, metricKey, limit));
    }

    /** 获取指标口径详情。 */
    @GetMapping("/{id}")
    public R<MetricDefinitionResp> getById(@PathVariable Long id) {
        return R.ok(service.getById(id));
    }

    /** 创建指标口径。 */
    @PostMapping
    public R<MetricDefinitionResp> create(@Valid @RequestBody MetricDefinitionReq req) {
        return R.ok(service.create(req));
    }

    /** 更新指标口径。 */
    @PutMapping("/{id}")
    public R<MetricDefinitionResp> update(@PathVariable Long id, @Valid @RequestBody MetricDefinitionReq req) {
        return R.ok(service.update(id, req));
    }

    /** 删除指标口径。 */
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return R.ok();
    }
}
