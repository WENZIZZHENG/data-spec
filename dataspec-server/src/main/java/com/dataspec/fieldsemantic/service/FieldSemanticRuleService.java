package com.dataspec.fieldsemantic.service;

import com.dataspec.fieldsemantic.model.FieldSemanticRuleReq;
import com.dataspec.fieldsemantic.model.FieldSemanticRuleResp;

import java.util.List;

/**
 * 字段语义规则服务，负责项目边界、字段引用归属和 secret-safe 文本校验。
 */
public interface FieldSemanticRuleService {

    default List<FieldSemanticRuleResp> list(Long projectId, Long fieldId, String ruleType, String query) {
        return list(projectId, fieldId, ruleType, query, null);
    }

    /**
     * 查询字段语义规则列表，limit 为可选返回上限，服务实现必须设置默认值和最大值，避免 AI Context 或工具入口全量拉取。
     */
    List<FieldSemanticRuleResp> list(Long projectId, Long fieldId, String ruleType, String query, Integer limit);

    /**
     * 查询与一组字段相关的语义规则，包含目标字段和 sourceField 引用；用于知识卡和 scoped AI Context，必须保持有界。
     */
    List<FieldSemanticRuleResp> listRelatedToFields(Long projectId, List<Long> fieldIds, Integer limit);

    FieldSemanticRuleResp getById(Long id);

    FieldSemanticRuleResp create(FieldSemanticRuleReq req);

    FieldSemanticRuleResp update(Long id, FieldSemanticRuleReq req);

    void delete(Long id);
}
