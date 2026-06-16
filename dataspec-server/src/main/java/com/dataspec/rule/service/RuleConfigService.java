package com.dataspec.rule.service;

import com.dataspec.rule.entity.RuleConfig;

import java.util.List;

public interface RuleConfigService {
    List<RuleConfig> listByProject(Long projectId);
    List<RuleConfig> listEnabledByProject(Long projectId);
    RuleConfig getById(Long id);
    RuleConfig create(RuleConfig ruleConfig);
    RuleConfig update(Long id, RuleConfig ruleConfig);
    void delete(Long id);
    void toggle(Long id, boolean enabled);
}
