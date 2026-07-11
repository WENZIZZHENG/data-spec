package com.dataspec.fieldsemantic.repository;

import com.dataspec.fieldsemantic.entity.FieldSemanticRule;

import java.util.List;
import java.util.Optional;

/**
 * 字段语义规则持久化端口，封装项目、字段和规则类型维度的查询。
 */
public interface FieldSemanticRuleRepository {

    Optional<FieldSemanticRule> findById(Long id);

    default List<FieldSemanticRule> findByProject(Long projectId, Long fieldId, String ruleType, String query) {
        return findByProject(projectId, fieldId, ruleType, query, null);
    }

    /**
     * 按项目查询语义规则，并在持久化层应用上限，防止绕过服务层的内部导出读取全量规则。
     */
    List<FieldSemanticRule> findByProject(Long projectId, Long fieldId, String ruleType, String query, Integer limit);

    /**
     * 按项目和字段集合查询相关语义规则，匹配目标字段或 sourceField，并在持久化层应用上限。
     */
    List<FieldSemanticRule> findRelatedToFields(Long projectId, List<Long> fieldIds, Integer limit);

    int insert(FieldSemanticRule rule);

    int update(FieldSemanticRule rule);

    int deleteById(Long id);
}
