package com.dataspec.rule.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dataspec.rule.entity.RuleConfig;
import com.dataspec.rule.mapper.RuleConfigMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 规则配置 Repository —— 封装规则配置的业务查询
 */

@Repository
@RequiredArgsConstructor
public class RuleConfigRepository {

    private final RuleConfigMapper ruleConfigMapper;

    /** 根据 ID 查找规则配置 */
    public Optional<RuleConfig> findById(Long id) {
        return Optional.ofNullable(ruleConfigMapper.selectById(id));
    }

    /** 查询项目下所有规则配置 */
    public List<RuleConfig> findByProjectId(Long projectId) {
        return ruleConfigMapper.selectList(
                new LambdaQueryWrapper<RuleConfig>()
                        .eq(RuleConfig::getProjectId, projectId)
                        .orderByAsc(RuleConfig::getRuleCode));
    }

    /** 查询项目下已启用的规则配置 */
    public List<RuleConfig> findEnabledByProjectId(Long projectId) {
        return ruleConfigMapper.selectList(
                new LambdaQueryWrapper<RuleConfig>()
                        .eq(RuleConfig::getProjectId, projectId)
                        .eq(RuleConfig::getEnabled, true)
                        .orderByAsc(RuleConfig::getRuleCode));
    }

    /** 根据规则编码和项目 ID 查找配置 */
    public Optional<RuleConfig> findByCodeAndProjectId(String ruleCode, Long projectId) {
        return Optional.ofNullable(ruleConfigMapper.selectOne(
                new LambdaQueryWrapper<RuleConfig>()
                        .eq(RuleConfig::getRuleCode, ruleCode)
                        .eq(RuleConfig::getProjectId, projectId)));
    }

    /** 新增规则配置 */
    public int insert(RuleConfig ruleConfig) {
        return ruleConfigMapper.insert(ruleConfig);
    }

    /** 更新规则配置 */
    public int update(RuleConfig ruleConfig) {
        return ruleConfigMapper.updateById(ruleConfig);
    }

    /** 逻辑删除规则配置 */
    public int deleteById(Long id) {
        return ruleConfigMapper.deleteById(id);
    }
}
