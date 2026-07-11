package com.dataspec.fieldknowledge.controller;

import com.dataspec.common.result.R;
import com.dataspec.fieldknowledge.model.FieldKnowledgeCardListResp;
import com.dataspec.fieldknowledge.model.FieldKnowledgeCardResp;
import com.dataspec.fieldknowledge.service.FieldKnowledgeCardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 字段知识卡 API，提供字段语义、枚举生命周期、命名翻译、使用示例和指标口径的只读聚合视图。
 */
@RestController
@RequestMapping("/api/field-knowledge-cards")
@RequiredArgsConstructor
public class FieldKnowledgeCardController {

    private final FieldKnowledgeCardService service;

    /** 查询字段知识卡列表，默认按 AI 可用性裁剪，避免一次性返回过多卡片。 */
    @GetMapping
    public R<FieldKnowledgeCardListResp> list(
            @RequestParam Long projectId,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long fieldId,
            @RequestParam(required = false) Integer limit) {
        return R.ok(service.list(projectId, query, status, fieldId, limit));
    }

    /** 获取单个字段知识卡详情。 */
    @GetMapping("/{fieldId}")
    public R<FieldKnowledgeCardResp> get(
            @PathVariable Long fieldId,
            @RequestParam Long projectId) {
        return R.ok(service.get(projectId, fieldId));
    }
}
