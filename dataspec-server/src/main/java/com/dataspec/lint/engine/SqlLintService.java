package com.dataspec.lint.engine;

import com.dataspec.aireplay.model.AiJobRecordCreateReq;
import com.dataspec.aireplay.service.AiJobRecordService;
import com.dataspec.aiprofile.service.AiTaskProfileService;
import com.dataspec.dialect.service.SqlDialectCompatibilityService;
import com.dataspec.lint.entity.SqlCheckRecord;
import com.dataspec.lint.model.*;
import com.dataspec.prompt.service.PromptTemplateRegistry;
import com.dataspec.lint.service.SqlCheckRecordService;
import com.dataspec.rule.entity.RuleConfig;
import com.dataspec.rule.service.RuleConfigService;
import com.dataspec.ruleexemption.service.RuleExemptionService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * SQL 校验服务 —— 编排 SQL 解析 + 规则执行 + 修正 SQL 生成 + 记录落库
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SqlLintService {

    private final SqlParserService sqlParserService;
    private final RuleConfigService ruleConfigService;
    private final List<LintRule> allRules; // Spring 自动注入所有 LintRule 实现
    private final ObjectMapper objectMapper;
    private final FixedSqlGenerator fixedSqlGenerator;
    private final SqlCheckRecordService sqlCheckRecordService;
    private final AiJobRecordService aiJobRecordService;
    private final RuleExemptionService ruleExemptionService;
    private final PromptTemplateRegistry promptTemplateRegistry;
    private final AiTaskProfileService aiTaskProfileService;
    private final SqlIssueSourceSpanResolver sourceSpanResolver = new SqlIssueSourceSpanResolver();
    private final SqlDiffGenerator sqlDiffGenerator = new SqlDiffGenerator();
    private final SqlDialectCompatibilityService dialectCompatibilityService = new SqlDialectCompatibilityService();

    /**
     * 校验 SQL（不指定项目，使用所有内置规则）
     */
    public LintResult lint(String sql) {
        return lint(sql, null, null);
    }

    /**
     * 校验 SQL（指定项目，根据项目规则配置过滤）
     */
    public LintResult lint(String sql, Long projectId) {
        return lint(sql, projectId, null);
    }

    /**
     * 校验 SQL（指定项目和 fixedSql 策略，根据项目规则配置过滤）
     */
    public LintResult lint(String sql, Long projectId, FixPolicy fixPolicy) {
        return lint(sql, projectId, fixPolicy, null, null);
    }

    /**
     * 校验 SQL（指定项目、fixedSql 策略和可选 AI profile）
     */
    public LintResult lint(String sql, Long projectId, FixPolicy fixPolicy, String profileId, String taskType) {
        FixPolicy effectiveFixPolicy = fixPolicy != null ? fixPolicy : profileFixPolicy(profileId, taskType);
        // 1. 解析 SQL
        List<TableDef> tables = sqlParserService.parse(sql);
        if (tables.isEmpty()) {
            LintResult emptyResult = LintResult.of(tables, List.of());
            applyFixedSqlPlan(emptyResult, fixedSqlGenerator.generatePlan(emptyResult, effectiveFixPolicy), sql);
            emptyResult.setDialectDiagnostics(dialectCompatibilityService.diagnoseSql(sql));
            // 无可解析表时仍落库一条记录,用于命中率统计与审计
            try {
                sqlCheckRecordService.save(projectId, sql, emptyResult);
            } catch (Exception e) {
                log.warn("保存 SQL 检查记录失败: {}", e.getMessage());
            }
            return emptyResult;
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
        sourceSpanResolver.resolve(sql, issues);
        ruleExemptionService.applySuppressions(projectId, issues);

        LintResult result = LintResult.of(tables, issues);
        // 基于确定性修复建议和请求级策略重建修正 SQL/计划
        applyFixedSqlPlan(result, fixedSqlGenerator.generatePlan(result, effectiveFixPolicy), sql);
        result.setDialectDiagnostics(dialectCompatibilityService.diagnoseSql(sql, result.getFixedSql() != null));

        // 落库检查记录(失败不阻断主流程,仅记录日志)
        SqlCheckRecord record = null;
        try {
            record = sqlCheckRecordService.save(projectId, sql, result);
        } catch (Exception e) {
            log.warn("保存 SQL 检查记录失败: {}", e.getMessage());
        }
        recordAiReplayJob(projectId, sql, result, record);

        return result;
    }

    private void applyFixedSqlPlan(LintResult result, FixedSqlPlan plan, String sql) {
        result.setFixPolicy(plan.getFixPolicy());
        result.setFixDryRun(plan.getFixDryRun());
        result.setFixChanges(plan.getFixChanges());
        result.setFixExplanations(plan.getFixExplanations());
        result.setFixSummary(plan.getFixSummary());
        result.setFixNextActions(plan.getFixNextActions());
        result.setFixedSql(plan.getFixedSql());
        result.setFixedSqlDiff(sqlDiffGenerator.generate(sql, plan.getFixedSql()));
    }

    private FixPolicy profileFixPolicy(String profileId, String taskType) {
        String profile = profileId != null && !profileId.isBlank() ? profileId : taskType;
        if (profile == null || profile.isBlank() || aiTaskProfileService == null) {
            return null;
        }
        return aiTaskProfileService.resolveFixedSqlPolicy(profile);
    }

    private void recordAiReplayJob(Long projectId, String sql, LintResult result, SqlCheckRecord record) {
        if (projectId == null) {
            return;
        }
        try {
            aiJobRecordService.create(new AiJobRecordCreateReq(
                    projectId,
                    "SQL_LINT_FIX",
                    "SQL 检查与修正",
                    summary(sql),
                    promptTemplateRegistry.promptVersion(PromptTemplateRegistry.SQL_LINT_FIX),
                    "SUCCESS",
                    orderedMap(
                            "sql", sql
                    ),
                    orderedMap(
                            "fixedSql", result.getFixedSql(),
                            "fixedSqlDiff", result.getFixedSqlDiff(),
                            "fixPolicy", result.getFixPolicy(),
                            "fixDryRun", result.getFixDryRun(),
                            "fixSummary", result.getFixSummary(),
                            "fixChanges", result.getFixChanges(),
                            "fixExplanations", result.getFixExplanations(),
                            "fixNextActions", result.getFixNextActions(),
                            "errorCount", result.getErrorCount(),
                            "warningCount", result.getWarningCount(),
                            "suggestionCount", result.getSuggestionCount(),
                            "dialectDiagnostics", result.getDialectDiagnostics(),
                            "issues", result.getIssues()
                    ),
                    record == null ? null : record.getStandardSnapshotId(),
                    record == null ? null : record.getStandardSnapshotVersion(),
                    record == null ? null : record.getStandardSnapshotHash(),
                    record == null ? null : record.getId()
            ));
        } catch (Exception e) {
            log.warn("保存 AI SQL 回放记录失败: {}", e.getMessage());
        }
    }

    private String summary(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        String normalized = text.trim().replaceAll("\\s+", " ");
        return normalized.length() <= 120 ? normalized : normalized.substring(0, 120);
    }

    private Map<String, Object> orderedMap(Object... keyValues) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            map.put(String.valueOf(keyValues[i]), keyValues[i + 1]);
        }
        return map;
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
