package com.dataspec.fieldknowledge.service;

import com.dataspec.fieldknowledge.model.FieldKnowledgeCardListResp;
import com.dataspec.fieldknowledge.model.FieldKnowledgeCardResp;

/**
 * 字段知识卡服务，提供只读聚合视图而不持久化长卡片正文。
 */
public interface FieldKnowledgeCardService {

    FieldKnowledgeCardListResp list(Long projectId, String query, String status, Long fieldId, Integer limit);

    FieldKnowledgeCardResp get(Long projectId, Long fieldId);
}
