package com.dataspec.rule.service.impl;

import com.dataspec.changelog.service.StandardChangeLogService;
import com.dataspec.common.exception.BizException;
import com.dataspec.rule.entity.RuleConfig;
import com.dataspec.rule.repository.RuleConfigRepository;
import com.dataspec.rule.service.RuleConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 规则配置服务实现
 */

@Service
@RequiredArgsConstructor
public class RuleConfigServiceImpl implements RuleConfigService {

    private final RuleConfigRepository ruleConfigRepository;
    private final StandardChangeLogService changeLogService;

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
        changeLogService.recordChange(
                ruleConfig.getProjectId(),
                StandardChangeLogService.TARGET_RULE_CONFIG,
                ruleConfig.getId(),
                StandardChangeLogService.ACTION_CREATE,
                null,
                changeLogService.snapshot(ruleConfig));
        return ruleConfig;
    }

    @Override
    public RuleConfig update(Long id, RuleConfig ruleConfig) {
        RuleConfig existing = getById(id);
        String beforeJson = changeLogService.snapshot(existing);
        existing.setRuleName(ruleConfig.getRuleName());
        existing.setSeverity(ruleConfig.getSeverity());
        existing.setEnabled(ruleConfig.getEnabled());
        existing.setParamsJson(ruleConfig.getParamsJson());
        ruleConfigRepository.update(existing);
        changeLogService.recordChange(
                existing.getProjectId(),
                StandardChangeLogService.TARGET_RULE_CONFIG,
                existing.getId(),
                StandardChangeLogService.ACTION_UPDATE,
                beforeJson,
                changeLogService.snapshot(existing));
        return existing;
    }

    @Override
    public void delete(Long id) {
        RuleConfig existing = getById(id);
        String beforeJson = changeLogService.snapshot(existing);
        ruleConfigRepository.deleteById(id);
        changeLogService.recordChange(
                existing.getProjectId(),
                StandardChangeLogService.TARGET_RULE_CONFIG,
                existing.getId(),
                StandardChangeLogService.ACTION_DELETE,
                beforeJson,
                null);
    }

    @Override
    public void toggle(Long id, boolean enabled) {
        RuleConfig existing = getById(id);
        String beforeJson = changeLogService.snapshot(existing);
        existing.setEnabled(enabled);
        ruleConfigRepository.update(existing);
        changeLogService.recordChange(
                existing.getProjectId(),
                StandardChangeLogService.TARGET_RULE_CONFIG,
                existing.getId(),
                StandardChangeLogService.ACTION_TOGGLE,
                beforeJson,
                changeLogService.snapshot(existing));
    }
}
