package com.dataspec.rulebaseline.service.impl;

import com.dataspec.common.exception.BizException;
import com.dataspec.rule.entity.RuleConfig;
import com.dataspec.rule.repository.RuleConfigRepository;
import com.dataspec.rule.service.RuleConfigService;
import com.dataspec.rulebaseline.entity.RuleBaseline;
import com.dataspec.rulebaseline.model.RuleBaselineApplyResult;
import com.dataspec.rulebaseline.model.RuleBaselineInfo;
import com.dataspec.rulebaseline.model.RuleBaselinePackage;
import com.dataspec.rulebaseline.model.RuleBaselineRule;
import com.dataspec.rulebaseline.model.RuleBaselineTemplate;
import com.dataspec.rulebaseline.repository.RuleBaselineRepository;
import com.dataspec.rulebaseline.service.BuiltInRuleBaselines;
import com.dataspec.rulebaseline.service.RuleBaselineService;
import com.dataspec.security.context.ProjectAccessGuard;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RuleBaselineServiceImpl implements RuleBaselineService {

    private static final String SOURCE_BUILT_IN = "built_in";
    private static final String SOURCE_IMPORTED = "imported";
    private static final String SOURCE_INFERRED = "inferred";
    private static final String CUSTOM_KEY = "custom";
    private static final String CUSTOM_NAME = "自定义规则";
    private static final String CUSTOM_VERSION = "unversioned";
    private static final Set<String> VALID_SEVERITIES = Set.of("ERROR", "WARNING", "SUGGESTION");

    private final RuleBaselineRepository ruleBaselineRepository;
    private final RuleConfigRepository ruleConfigRepository;
    private final RuleConfigService ruleConfigService;
    private final ObjectMapper objectMapper;

    @Override
    public List<RuleBaselineTemplate> listTemplates() {
        return BuiltInRuleBaselines.list();
    }

    @Override
    public RuleBaselineInfo currentBaseline(Long projectId) {
        ProjectAccessGuard.requireProjectAccess(projectId);
        return ruleBaselineRepository.findByProjectId(projectId)
                .map(entity -> toInfo(entity, currentRuleCount(projectId)))
                .orElseGet(() -> inferredBaseline(projectId, currentRuleCount(projectId)));
    }

    @Override
    @Transactional
    public RuleBaselineApplyResult applyBuiltInBaseline(Long projectId, String baselineKey, boolean overwrite) {
        ProjectAccessGuard.requireProjectAccess(projectId);
        RuleBaselineTemplate template = BuiltInRuleBaselines.find(baselineKey)
                .orElseThrow(() -> new BizException("规则基线不存在: " + baselineKey));
        return applyRules(
                projectId,
                template.key(),
                template.name(),
                template.version(),
                SOURCE_BUILT_IN,
                template.rules(),
                overwrite);
    }

    @Override
    public RuleBaselinePackage exportBaseline(Long projectId) {
        ProjectAccessGuard.requireProjectAccess(projectId);
        List<RuleBaselineRule> rules = ruleConfigRepository.findByProjectId(projectId).stream()
                .sorted(Comparator.comparing(RuleConfig::getRuleCode, Comparator.nullsLast(String::compareTo)))
                .map(this::toRule)
                .toList();
        RuleBaselineInfo baseline = ruleBaselineRepository.findByProjectId(projectId)
                .map(entity -> toInfo(entity, rules.size()))
                .orElseGet(() -> inferredBaseline(projectId, rules.size()));
        return new RuleBaselinePackage(BuiltInRuleBaselines.SCHEMA_VERSION, baseline, LocalDateTime.now(), rules);
    }

    @Override
    @Transactional
    public RuleBaselineApplyResult importBaseline(Long projectId, RuleBaselinePackage baselinePackage, boolean overwrite) {
        ProjectAccessGuard.requireProjectAccess(projectId);
        validatePackage(baselinePackage);
        RuleBaselineInfo imported = baselinePackage.baseline();
        String key = textOrDefault(imported == null ? null : imported.key(), CUSTOM_KEY);
        String name = textOrDefault(imported == null ? null : imported.name(), "导入规则基线");
        String version = textOrDefault(imported == null ? null : imported.version(), CUSTOM_VERSION);
        return applyRules(projectId, key, name, version, SOURCE_IMPORTED, baselinePackage.rules(), overwrite);
    }

    private RuleBaselineApplyResult applyRules(
            Long projectId,
            String baselineKey,
            String baselineName,
            String baselineVersion,
            String source,
            List<RuleBaselineRule> rules,
            boolean overwrite
    ) {
        validateRules(rules);
        List<String> created = new ArrayList<>();
        List<String> updated = new ArrayList<>();
        List<String> skipped = new ArrayList<>();

        for (RuleBaselineRule rule : rules) {
            RuleConfig existing = ruleConfigRepository.findByCodeAndProjectId(rule.ruleCode(), projectId).orElse(null);
            if (existing == null) {
                RuleConfig createdRule = toRuleConfig(projectId, rule);
                ruleConfigService.create(createdRule);
                created.add(rule.ruleCode());
            } else if (overwrite) {
                RuleConfig update = toRuleConfig(projectId, rule);
                ruleConfigService.update(existing.getId(), update);
                updated.add(rule.ruleCode());
            } else {
                skipped.add(rule.ruleCode());
            }
        }

        RuleBaseline baseline = saveBaseline(projectId, baselineKey, baselineName, baselineVersion, source, rules);
        RuleBaselineInfo info = toInfo(baseline, currentRuleCount(projectId));
        return new RuleBaselineApplyResult(
                projectId,
                info,
                created.size(),
                updated.size(),
                skipped.size(),
                created,
                updated,
                skipped);
    }

    private RuleBaseline saveBaseline(
            Long projectId,
            String baselineKey,
            String baselineName,
            String baselineVersion,
            String source,
            List<RuleBaselineRule> rules
    ) {
        LocalDateTime appliedAt = LocalDateTime.now();
        String rulesJson = serializePackage(projectId, baselineKey, baselineName, baselineVersion, source, appliedAt, rules);
        RuleBaseline baseline = ruleBaselineRepository.findByProjectId(projectId).orElseGet(RuleBaseline::new);
        baseline.setProjectId(projectId);
        baseline.setBaselineKey(baselineKey);
        baseline.setBaselineName(baselineName);
        baseline.setBaselineVersion(baselineVersion);
        baseline.setSource(source);
        baseline.setAppliedAt(appliedAt);
        baseline.setRulesJson(rulesJson);
        if (baseline.getId() == null) {
            ruleBaselineRepository.insert(baseline);
        } else {
            ruleBaselineRepository.update(baseline);
        }
        return baseline;
    }

    private String serializePackage(
            Long projectId,
            String baselineKey,
            String baselineName,
            String baselineVersion,
            String source,
            LocalDateTime appliedAt,
            List<RuleBaselineRule> rules
    ) {
        try {
            RuleBaselineInfo info = new RuleBaselineInfo(
                    projectId,
                    baselineKey,
                    baselineName,
                    baselineVersion,
                    source,
                    appliedAt,
                    rules.size());
            RuleBaselinePackage pkg = new RuleBaselinePackage(
                    BuiltInRuleBaselines.SCHEMA_VERSION,
                    info,
                    appliedAt,
                    rules);
            return objectMapper.copy().findAndRegisterModules().writeValueAsString(pkg);
        } catch (JsonProcessingException e) {
            throw new BizException("规则基线序列化失败: " + e.getMessage());
        }
    }

    private void validatePackage(RuleBaselinePackage pkg) {
        if (pkg == null) {
            throw new BizException("基线包不能为空");
        }
        if (pkg.schemaVersion() == null || pkg.schemaVersion() != BuiltInRuleBaselines.SCHEMA_VERSION) {
            throw new BizException("不支持的规则基线 schemaVersion: " + pkg.schemaVersion());
        }
        validateRules(pkg.rules());
    }

    private void validateRules(List<RuleBaselineRule> rules) {
        if (rules == null || rules.isEmpty()) {
            throw new BizException("规则基线至少需要包含一条规则");
        }
        for (RuleBaselineRule rule : rules) {
            if (rule == null || isBlank(rule.ruleCode()) || isBlank(rule.ruleName())) {
                throw new BizException("规则基线包含无效规则: ruleCode/ruleName 不能为空");
            }
            validateSeverity(rule);
            validateParamsJson(rule);
        }
    }

    private void validateSeverity(RuleBaselineRule rule) {
        String severity = textOrDefault(rule.severity(), "WARNING").trim().toUpperCase();
        if (!VALID_SEVERITIES.contains(severity)) {
            throw new BizException("规则 " + rule.ruleCode() + " 的 severity 无效: " + rule.severity());
        }
    }

    private void validateParamsJson(RuleBaselineRule rule) {
        String paramsJson = textOrDefault(rule.paramsJson(), "{}");
        try {
            objectMapper.readTree(paramsJson);
        } catch (Exception e) {
            throw new BizException("规则 " + rule.ruleCode() + " 的 paramsJson 不是合法 JSON");
        }
    }

    private RuleConfig toRuleConfig(Long projectId, RuleBaselineRule rule) {
        RuleConfig config = new RuleConfig();
        config.setProjectId(projectId);
        config.setRuleCode(rule.ruleCode().trim());
        config.setRuleName(rule.ruleName().trim());
        config.setSeverity(textOrDefault(rule.severity(), "WARNING").trim().toUpperCase());
        config.setEnabled(rule.enabled() == null || rule.enabled());
        config.setParamsJson(textOrDefault(rule.paramsJson(), "{}"));
        return config;
    }

    private RuleBaselineRule toRule(RuleConfig config) {
        return new RuleBaselineRule(
                config.getRuleCode(),
                config.getRuleName(),
                config.getSeverity(),
                config.getEnabled(),
                textOrDefault(config.getParamsJson(), "{}"));
    }

    private RuleBaselineInfo toInfo(RuleBaseline entity, int ruleCount) {
        return new RuleBaselineInfo(
                entity.getProjectId(),
                entity.getBaselineKey(),
                entity.getBaselineName(),
                entity.getBaselineVersion(),
                entity.getSource(),
                entity.getAppliedAt(),
                ruleCount);
    }

    private RuleBaselineInfo inferredBaseline(Long projectId, int ruleCount) {
        return new RuleBaselineInfo(
                projectId,
                CUSTOM_KEY,
                CUSTOM_NAME,
                CUSTOM_VERSION,
                SOURCE_INFERRED,
                null,
                ruleCount);
    }

    private int currentRuleCount(Long projectId) {
        return ruleConfigRepository.findByProjectId(projectId).size();
    }

    private String textOrDefault(String value, String defaultValue) {
        return isBlank(value) ? defaultValue : value;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
