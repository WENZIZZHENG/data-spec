package com.dataspec.ruleexemption.service.impl;

import com.dataspec.common.exception.BizException;
import com.dataspec.lint.model.LintIssue;
import com.dataspec.ruleexemption.entity.RuleExemption;
import com.dataspec.ruleexemption.repository.RuleExemptionRepository;
import com.dataspec.ruleexemption.service.RuleExemptionService;
import com.dataspec.security.context.ProjectAccessGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * 规则豁免服务实现。
 */
@Service
@RequiredArgsConstructor
public class RuleExemptionServiceImpl implements RuleExemptionService {

    private final RuleExemptionRepository ruleExemptionRepository;

    @Override
    public List<RuleExemption> listByProject(Long projectId) {
        ProjectAccessGuard.requireProjectAccess(projectId);
        return ruleExemptionRepository.findByProjectId(projectId);
    }

    @Override
    public List<RuleExemption> listActiveByProject(Long projectId) {
        LocalDateTime now = LocalDateTime.now();
        return listByProject(projectId).stream()
                .filter(exemption -> Boolean.TRUE.equals(exemption.getEnabled()))
                .filter(exemption -> exemption.getExpiresAt() == null || exemption.getExpiresAt().isAfter(now))
                .toList();
    }

    @Override
    public RuleExemption getById(Long id) {
        RuleExemption exemption = ruleExemptionRepository.findById(id)
                .orElseThrow(() -> new BizException("规则豁免不存在: " + id));
        ProjectAccessGuard.requireProjectAccess(exemption.getProjectId());
        return exemption;
    }

    @Override
    public RuleExemption create(RuleExemption exemption) {
        normalizeAndValidate(exemption);
        ProjectAccessGuard.requireProjectAccess(exemption.getProjectId());
        exemption.setEnabled(exemption.getEnabled() != null ? exemption.getEnabled() : true);
        ruleExemptionRepository.insert(exemption);
        return exemption;
    }

    @Override
    public void disable(Long id) {
        RuleExemption exemption = getById(id);
        exemption.setEnabled(false);
        ruleExemptionRepository.update(exemption);
    }

    @Override
    public void delete(Long id) {
        RuleExemption exemption = getById(id);
        ruleExemptionRepository.deleteById(exemption.getId());
    }

    @Override
    public void applySuppressions(Long projectId, List<LintIssue> issues) {
        if (projectId == null || issues == null || issues.isEmpty()) {
            return;
        }
        List<RuleExemption> activeExemptions = listActiveByProject(projectId);
        if (activeExemptions.isEmpty()) {
            return;
        }
        for (LintIssue issue : issues) {
            activeExemptions.stream()
                    .filter(exemption -> matches(exemption, issue))
                    .findFirst()
                    .ifPresent(exemption -> suppress(issue, exemption));
        }
    }

    private void normalizeAndValidate(RuleExemption exemption) {
        if (exemption == null) {
            throw new BizException("规则豁免不能为空");
        }
        if (exemption.getProjectId() == null) {
            throw new BizException("项目ID不能为空");
        }
        exemption.setRuleCode(trimToNull(exemption.getRuleCode()));
        exemption.setTableName(trimToNull(exemption.getTableName()));
        exemption.setColumnName(trimToNull(exemption.getColumnName()));
        exemption.setReason(trimToNull(exemption.getReason()));
        if (exemption.getRuleCode() == null) {
            throw new BizException("规则编码不能为空");
        }
        if (exemption.getReason() == null) {
            throw new BizException("豁免原因不能为空");
        }
        if (exemption.getTableName() == null && exemption.getColumnName() == null) {
            throw new BizException("规则豁免必须指定表名或字段名范围");
        }
    }

    private boolean matches(RuleExemption exemption, LintIssue issue) {
        return Objects.equals(exemption.getRuleCode(), issue.getRuleCode())
                && matchesScope(exemption.getTableName(), issue.getTableName())
                && matchesScope(exemption.getColumnName(), issue.getColumnName());
    }

    private boolean matchesScope(String exemptionScope, String issueScope) {
        return exemptionScope == null || Objects.equals(exemptionScope, issueScope);
    }

    private void suppress(LintIssue issue, RuleExemption exemption) {
        issue.setSuppressed(true);
        issue.setSuppressionId(exemption.getId());
        issue.setSuppressionReason(exemption.getReason());
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
