package com.dataspec.ruleexemption.service;

import com.dataspec.lint.model.LintIssue;
import com.dataspec.ruleexemption.entity.RuleExemption;

import java.util.List;

/**
 * 项目级规则豁免服务。
 */
public interface RuleExemptionService {

    List<RuleExemption> listByProject(Long projectId);

    List<RuleExemption> listActiveByProject(Long projectId);

    RuleExemption getById(Long id);

    RuleExemption create(RuleExemption exemption);

    void disable(Long id);

    void delete(Long id);

    /**
     * 对 lint issue 应用项目例外。
     * <p>
     * 命中的 issue 会保留在结果中并标记 suppressed，方便 AI 和用户继续看到历史例外，
     * 但后续 active 统计会排除这些 issue。
     */
    void applySuppressions(Long projectId, List<LintIssue> issues);
}
