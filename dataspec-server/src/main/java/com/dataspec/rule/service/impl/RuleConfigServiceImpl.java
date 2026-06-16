package com.dataspec.rule.service.impl;

import com.dataspec.common.exception.BizException;
import com.dataspec.rule.entity.RuleConfig;
import com.dataspec.rule.repository.RuleConfigRepository;
import com.dataspec.rule.service.RuleConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RuleConfigServiceImpl implements RuleConfigService {

    private final RuleConfigRepository ruleConfigRepository;

    @Override
    public List<RuleConfig> listByProject(Long projectId) {
        return ruleConfigRepository.findByProjectId(projectId);
    }

    @Override
    public List<RuleConfig> listEnabledByProject(Long projectId) {
        return ruleConfigRepository.findEnabledByProjectId(projectId);
    }

    @Override
    public RuleConfig getById(Long id) {
        return ruleConfigRepository.findById(id)
                .orElseThrow(() -> new BizException("规则配置不存在: " + id));
    }

    @Override
    public RuleConfig create(RuleConfig ruleConfig) {
        ruleConfig.setEnabled(ruleConfig.getEnabled() != null ? ruleConfig.getEnabled() : true);
        ruleConfig.setSeverity(ruleConfig.getSeverity() != null ? ruleConfig.getSeverity() : "warning");
        ruleConfigRepository.insert(ruleConfig);
        return ruleConfig;
    }

    @Override
    public RuleConfig update(Long id, RuleConfig ruleConfig) {
        RuleConfig existing = getById(id);
        existing.setRuleName(ruleConfig.getRuleName());
        existing.setSeverity(ruleConfig.getSeverity());
        existing.setEnabled(ruleConfig.getEnabled());
        existing.setParamsJson(ruleConfig.getParamsJson());
        ruleConfigRepository.update(existing);
        return existing;
    }

    @Override
    public void delete(Long id) {
        getById(id);
        ruleConfigRepository.deleteById(id);
    }

    @Override
    public void toggle(Long id, boolean enabled) {
        RuleConfig existing = getById(id);
        existing.setEnabled(enabled);
        ruleConfigRepository.update(existing);
    }
}
