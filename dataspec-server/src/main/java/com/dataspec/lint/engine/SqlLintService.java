package com.dataspec.lint.engine;

import com.dataspec.lint.model.*;
import com.dataspec.rule.entity.RuleConfig;
import com.dataspec.rule.service.RuleConfigService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * SQL 校验服务 —— 编排 SQL 解析 + 规则执行
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SqlLintService {

    private final SqlParserService sqlParserService;
    private final RuleConfigService ruleConfigService;
    private final List<LintRule> allRules; // Spring 自动注入所有 LintRule 实现
    private final ObjectMapper objectMapper;

    /**
     * 校验 SQL（不指定项目，使用所有内置规则）
     */
    public LintResult lint(String sql) {
        return lint(sql, null);
    }

    /**
     * 校验 SQL（指定项目，根据项目规则配置过滤）
     */
    public LintResult lint(String sql, Long projectId) {
        // 1. 解析 SQL
        List<TableDef> tables = sqlParserService.parse(sql);
        if (tables.isEmpty()) {
            return LintResult.of(tables, List.of());
        }

        // 2. 确定启用的规则
        Map<String, RuleConfig> configMap = new HashMap<>();
        if (projectId != null) {
            List<RuleConfig> configs = ruleConfigService.listEnabledByProject(projectId);
            for (RuleConfig cfg : configs) {
                configMap.put(cfg.getRuleCode(), cfg);
            }
        }
        boolean hasProjectRuleConfig = !configMap.isEmpty();

        // 3. 执行规则
        List<LintIssue> issues = new ArrayList<>();
        for (LintRule rule : allRules) {
            // 有项目规则配置时，只执行配置中启用的规则；新项目无配置时回退到内置规则
            if (hasProjectRuleConfig && !configMap.containsKey(rule.getCode())) {
                continue;
            }

            // 解析规则参数
            Map<String, Object> params = Collections.emptyMap();
            RuleConfig cfg = configMap.get(rule.getCode());
            if (cfg != null && cfg.getParamsJson() != null && !cfg.getParamsJson().isBlank()) {
                try {
                    params = objectMapper.readValue(cfg.getParamsJson(),
                            new TypeReference<Map<String, Object>>() {});
                } catch (Exception e) {
                    log.warn("规则 {} 参数解析失败: {}", rule.getCode(), e.getMessage());
                }
            }

            // 用配置中的 severity 覆盖默认 severity
            Severity overrideSeverity = null;
            if (cfg != null && cfg.getSeverity() != null) {
                try {
                    overrideSeverity = Severity.valueOf(cfg.getSeverity().toUpperCase());
                } catch (IllegalArgumentException ignored) {
                }
            }

            RuleContext context = RuleContext.builder()
                    .tables(tables)
                    .projectId(projectId)
                    .ruleParams(params)
                    .build();

            List<LintIssue> ruleIssues;
            try {
                ruleIssues = rule.check(context);
            } catch (Exception e) {
                log.error("规则 {} 执行异常: {}", rule.getCode(), e.getMessage(), e);
                ruleIssues = List.of(LintIssue.builder()
                        .severity(Severity.WARNING)
                        .ruleCode(rule.getCode())
                        .ruleName(rule.getName())
                        .message(String.format("规则 '%s' 执行异常: %s", rule.getName(), e.getMessage()))
                        .build());
            }

            // 应用 severity 覆盖
            if (overrideSeverity != null) {
                for (LintIssue issue : ruleIssues) {
                    issue.setSeverity(overrideSeverity);
                }
            }

            issues.addAll(ruleIssues);
        }

        return LintResult.of(tables, issues);
    }

    /**
     * 获取所有可用规则的信息
     */
    public List<Map<String, String>> listAvailableRules() {
        return allRules.stream()
                .map(r -> Map.of("code", r.getCode(), "name", r.getName()))
                .toList();
    }
}
