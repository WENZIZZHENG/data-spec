package com.dataspec.rulebaseline.service;

import com.dataspec.rulebaseline.model.RuleBaselineApplyResult;
import com.dataspec.rulebaseline.model.RuleBaselineInfo;
import com.dataspec.rulebaseline.model.RuleBaselinePackage;
import com.dataspec.rulebaseline.model.RuleBaselineTemplate;

import java.util.List;

public interface RuleBaselineService {

    List<RuleBaselineTemplate> listTemplates();

    RuleBaselineInfo currentBaseline(Long projectId);

    RuleBaselineApplyResult applyBuiltInBaseline(Long projectId, String baselineKey, boolean overwrite);

    RuleBaselinePackage exportBaseline(Long projectId);

    RuleBaselineApplyResult importBaseline(Long projectId, RuleBaselinePackage baselinePackage, boolean overwrite);
}
