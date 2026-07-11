package com.dataspec.fieldsemantic.controller;

import com.dataspec.common.result.R;
import com.dataspec.fieldsemantic.model.FieldSemanticRuleReq;
import com.dataspec.fieldsemantic.model.FieldSemanticRuleResp;
import com.dataspec.fieldsemantic.service.FieldSemanticRuleService;
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
 * 字段语义规则 API，提供字段派生、单位换算、聚合口径和 source of truth guidance 的项目级维护入口。
 */
@RestController
@RequestMapping("/api/field-semantics")
@RequiredArgsConstructor
public class FieldSemanticRuleController {

    private final FieldSemanticRuleService service;

    /** 查询字段语义规则列表。 */
    @GetMapping
    public R<List<FieldSemanticRuleResp>> list(
            @RequestParam Long projectId,
            @RequestParam(required = false) Long fieldId,
            @RequestParam(required = false) String ruleType,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Integer limit) {
        return R.ok(service.list(projectId, fieldId, ruleType, query, limit));
    }

    /** 获取字段语义规则详情。 */
    @GetMapping("/{id}")
    public R<FieldSemanticRuleResp> getById(@PathVariable Long id) {
        return R.ok(service.getById(id));
    }

    /** 创建字段语义规则。 */
    @PostMapping
    public R<FieldSemanticRuleResp> create(@Valid @RequestBody FieldSemanticRuleReq req) {
        return R.ok(service.create(req));
    }

    /** 更新字段语义规则。 */
    @PutMapping("/{id}")
    public R<FieldSemanticRuleResp> update(@PathVariable Long id, @Valid @RequestBody FieldSemanticRuleReq req) {
        return R.ok(service.update(id, req));
    }

    /** 删除字段语义规则。 */
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return R.ok();
    }
}
