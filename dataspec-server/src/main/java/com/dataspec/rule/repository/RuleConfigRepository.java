package com.dataspec.rule.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dataspec.rule.entity.RuleConfig;
import com.dataspec.rule.mapper.RuleConfigMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class RuleConfigRepository {

    private final RuleConfigMapper ruleConfigMapper;

    public Optional<RuleConfig> findById(Long id) {
        return Optional.ofNullable(ruleConfigMapper.selectById(id));
    }

    public List<RuleConfig> findByProjectId(Long projectId) {
        return ruleConfigMapper.selectList(
                new LambdaQueryWrapper<RuleConfig>()
                        .eq(RuleConfig::getProjectId, projectId)
                        .orderByAsc(RuleConfig::getRuleCode));
    }

    public List<RuleConfig> findEnabledByProjectId(Long projectId) {
        return ruleConfigMapper.selectList(
                new LambdaQueryWrapper<RuleConfig>()
                        .eq(RuleConfig::getProjectId, projectId)
                        .eq(RuleConfig::getEnabled, true)
                        .orderByAsc(RuleConfig::getRuleCode));
    }

    public Optional<RuleConfig> findByCodeAndProjectId(String ruleCode, Long projectId) {
        return Optional.ofNullable(ruleConfigMapper.selectOne(
                new LambdaQueryWrapper<RuleConfig>()
                        .eq(RuleConfig::getRuleCode, ruleCode)
                        .eq(RuleConfig::getProjectId, projectId)));
    }

    public int insert(RuleConfig ruleConfig) {
        return ruleConfigMapper.insert(ruleConfig);
    }

    public int update(RuleConfig ruleConfig) {
        return ruleConfigMapper.updateById(ruleConfig);
    }

    public int deleteById(Long id) {
        return ruleConfigMapper.deleteById(id);
    }
}
