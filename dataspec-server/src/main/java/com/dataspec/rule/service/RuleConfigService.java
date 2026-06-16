package com.dataspec.rule.service;

import com.dataspec.rule.entity.RuleConfig;

import java.util.List;

/**
 * 规则配置服务接口
 */
public interface RuleConfigService {
    List<RuleConfig> listByProject(Long projectId);
    List<RuleConfig> listEnabledByProject(Long projectId);
    RuleConfig getById(Long id);
    RuleConfig create(RuleConfig ruleConfig);
    RuleConfig update(Long id, RuleConfig ruleConfig);
    void delete(Long id);
    void toggle(Long id, boolean enabled);
}
