package com.dataspec.fieldsemantic.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dataspec.fieldsemantic.entity.FieldSemanticRule;
import com.dataspec.fieldsemantic.mapper.FieldSemanticRuleMapper;
import com.dataspec.fieldsemantic.repository.FieldSemanticRuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 基于 MyBatis 的字段语义规则持久化实现。
 */
@Repository
@RequiredArgsConstructor
public class FieldSemanticRuleRepositoryImpl implements FieldSemanticRuleRepository {

    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 100;

    private final FieldSemanticRuleMapper mapper;

    @Override
    public Optional<FieldSemanticRule> findById(Long id) {
        return Optional.ofNullable(mapper.selectById(id));
    }

    @Override
    public List<FieldSemanticRule> findByProject(Long projectId, Long fieldId, String ruleType, String query, Integer limit) {
        LambdaQueryWrapper<FieldSemanticRule> wrapper = new LambdaQueryWrapper<FieldSemanticRule>()
                .eq(FieldSemanticRule::getProjectId, projectId)
                .orderByAsc(FieldSemanticRule::getFieldId)
                .orderByAsc(FieldSemanticRule::getRuleType)
                .orderByAsc(FieldSemanticRule::getId)
                .last("LIMIT " + safeLimit(limit));
        if (fieldId != null) {
            wrapper.eq(FieldSemanticRule::getFieldId, fieldId);
        }
        if (ruleType != null && !ruleType.isBlank()) {
            wrapper.eq(FieldSemanticRule::getRuleType, ruleType);
        }
        if (query != null && !query.isBlank()) {
            String like = query.trim();
            wrapper.and(nested -> nested
                    .like(FieldSemanticRule::getUnitConversion, like)
                    .or()
                    .like(FieldSemanticRule::getAggregationRule, like)
                    .or()
                    .like(FieldSemanticRule::getSourceOfTruth, like)
                    .or()
                    .like(FieldSemanticRule::getRecommendedUse, like)
                    .or()
                    .like(FieldSemanticRule::getAntiPatterns, like));
        }
        return mapper.selectList(wrapper);
    }

    @Override
    public List<FieldSemanticRule> findRelatedToFields(Long projectId, List<Long> fieldIds, Integer limit) {
        if (fieldIds == null || fieldIds.isEmpty()) {
            return List.of();
        }
        LambdaQueryWrapper<FieldSemanticRule> wrapper = new LambdaQueryWrapper<FieldSemanticRule>()
                .eq(FieldSemanticRule::getProjectId, projectId)
                .and(nested -> nested
                        .in(FieldSemanticRule::getFieldId, fieldIds)
                        .or()
                        .in(FieldSemanticRule::getSourceFieldId, fieldIds))
                .orderByAsc(FieldSemanticRule::getFieldId)
                .orderByAsc(FieldSemanticRule::getRuleType)
                .orderByAsc(FieldSemanticRule::getId)
                .last("LIMIT " + safeLimit(limit));
        return mapper.selectList(wrapper);
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
    public int insert(FieldSemanticRule rule) {
        return mapper.insert(rule);
    }

    @Override
    public int update(FieldSemanticRule rule) {
        return mapper.updateById(rule);
    }

    @Override
    public int deleteById(Long id) {
        return mapper.deleteById(id);
    }
}
