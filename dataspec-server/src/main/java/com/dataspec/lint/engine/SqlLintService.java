package com.dataspec.lint.engine;

import com.dataspec.aireplay.model.AiJobRecordCreateReq;
import com.dataspec.aireplay.service.AiJobRecordService;
import com.dataspec.aiprofile.service.AiTaskProfileService;
import com.dataspec.common.sanitize.SensitiveDataSanitizer;
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

    private static final String DEBUG_VERSION = "sql-rule-debug@1";

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
        LintExecution execution = executeLint(sql, projectId, effectiveFixPolicy, false);
        LintResult result = execution.result();

        // 落库检查记录(失败不阻断主流程,仅记录日志)
        SqlCheckRecord record = null;
        try {
            record = sqlCheckRecordService.save(projectId, sql, result);
        } catch (Exception e) {
            log.warn("保存 SQL 检查记录失败: {}", e.getMessage());
        }
        if (result.getTables() != null && !result.getTables().isEmpty()) {
            recordAiReplayJob(projectId, sql, result, record);
        }

        return result;
    }

    /**
     * 生成 SQL 规则调试结果。
     * <p>
     * 调试接口复用 lint 规则执行、source range、豁免和 fixedSql 计划逻辑，但不保存检查记录、
     * 不记录 AI replay，避免只读排障动作产生项目状态副作用。
     */
    public SqlLintDebugResult debug(String sql, Long projectId, FixPolicy fixPolicy, String profileId, String taskType) {
        FixPolicy effectiveFixPolicy = fixPolicy != null ? fixPolicy : profileFixPolicy(profileId, taskType);
        LintExecution execution = executeLint(sql, projectId, effectiveFixPolicy, true);
        List<String> notes = new ArrayList<>();
        notes.add("规则调试为只读执行，不保存 SQL 检查记录，也不创建规则豁免或修改规则配置。");
        if (execution.result().getTables() == null || execution.result().getTables().isEmpty()) {
            notes.add("SQL 未解析到 CREATE TABLE，规则执行 trace 仅反映启用状态。");
        }
        return SqlLintDebugResult.builder()
                .debugVersion(DEBUG_VERSION)
                .lintResult(execution.result())
                .rules(execution.debugRules())
                .debugNotes(notes)
                .build();
    }

    private LintExecution executeLint(String sql, Long projectId, FixPolicy effectiveFixPolicy, boolean collectDebug) {
        List<TableDef> tables = sqlParserService.parse(sql);
        RuleSettings ruleSettings = loadRuleSettings(projectId);
        List<RuleExecutionSnapshot> snapshots = new ArrayList<>();
        List<LintIssue> issues = new ArrayList<>();

        if (!tables.isEmpty()) {
            for (LintRule rule : allRules) {
                RuleRuntimeConfig runtimeConfig = runtimeConfig(ruleSettings, rule);
                if (!runtimeConfig.enabled()) {
                    if (collectDebug) {
                        snapshots.add(RuleExecutionSnapshot.disabled(rule, runtimeConfig));
                    }
                    continue;
                }

                RuleContext context = RuleContext.builder()
                        .tables(tables)
                        .projectId(projectId)
                        .ruleParams(runtimeConfig.params())
                        .build();

                List<LintIssue> ruleIssues;
                String executionError = null;
                try {
                    ruleIssues = rule.check(context);
                } catch (Exception e) {
                    log.error("规则 {} 执行异常: {}", rule.getCode(), e.getMessage(), e);
                    executionError = e.getMessage();
                    ruleIssues = List.of(LintIssue.builder()
                            .severity(Severity.WARNING)
                            .ruleCode(rule.getCode())
                            .ruleName(rule.getName())
                            .message(String.format("规则 '%s' 执行异常: %s", rule.getName(), e.getMessage()))
                            .build());
                }

                if (runtimeConfig.overrideSeverity() != null) {
                    for (LintIssue issue : ruleIssues) {
                        issue.setSeverity(runtimeConfig.overrideSeverity());
                    }
                }

                issues.addAll(ruleIssues);
                if (collectDebug) {
                    snapshots.add(RuleExecutionSnapshot.executed(rule, runtimeConfig, ruleIssues, executionError));
                }
            }
        } else if (collectDebug) {
            for (LintRule rule : allRules) {
                RuleRuntimeConfig runtimeConfig = runtimeConfig(ruleSettings, rule);
                snapshots.add(runtimeConfig.enabled()
                        ? RuleExecutionSnapshot.unparsed(rule, runtimeConfig)
                        : RuleExecutionSnapshot.disabled(rule, runtimeConfig));
            }
        }

        sourceSpanResolver.resolve(sql, issues);
        ruleExemptionService.applySuppressions(projectId, issues);

        LintResult result = LintResult.of(tables, issues);
        applyFixedSqlPlan(result, fixedSqlGenerator.generatePlan(result, effectiveFixPolicy), sql);
        result.setDialectDiagnostics(dialectCompatibilityService.diagnoseSql(sql, result.getFixedSql() != null));

        List<SqlRuleDebugTrace> debugRules = collectDebug ? buildDebugRules(snapshots, result) : List.of();
        return new LintExecution(result, debugRules);
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

    private RuleSettings loadRuleSettings(Long projectId) {
        if (projectId == null) {
            return new RuleSettings(false, Map.of());
        }
        Map<String, RuleConfig> configMap = new LinkedHashMap<>();
        for (RuleConfig cfg : ruleConfigService.listByProject(projectId)) {
            if (cfg.getRuleCode() != null && !cfg.getRuleCode().isBlank()) {
                configMap.put(cfg.getRuleCode(), cfg);
            }
        }
        return new RuleSettings(!configMap.isEmpty(), configMap);
    }

    private RuleRuntimeConfig runtimeConfig(RuleSettings settings, LintRule rule) {
        RuleConfig cfg = settings.configs().get(rule.getCode());
        List<String> debugNotes = new ArrayList<>();
        Map<String, Object> params = parseRuleParams(rule.getCode(), cfg, debugNotes);
        Severity overrideSeverity = parseSeverity(rule.getCode(), cfg, debugNotes);
        boolean enabled = !settings.hasProjectRuleConfig()
                || (cfg != null && !Boolean.FALSE.equals(cfg.getEnabled()));
        String disabledReason = null;
        if (!enabled) {
            disabledReason = cfg == null
                    ? "项目已有规则配置，但未启用该规则。"
                    : "项目规则配置已禁用该规则。";
        }
        return new RuleRuntimeConfig(cfg, enabled, params, overrideSeverity, debugNotes, disabledReason);
    }

    private Map<String, Object> parseRuleParams(String ruleCode, RuleConfig cfg, List<String> debugNotes) {
        if (cfg == null || cfg.getParamsJson() == null || cfg.getParamsJson().isBlank()) {
            return Collections.emptyMap();
        }
        try {
            return objectMapper.readValue(cfg.getParamsJson(), new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.warn("规则 {} 参数解析失败: {}", ruleCode, e.getMessage());
            debugNotes.add("规则参数 JSON 解析失败，已使用空参数快照。");
            return Collections.emptyMap();
        }
    }

    private Severity parseSeverity(String ruleCode, RuleConfig cfg, List<String> debugNotes) {
        if (cfg == null || cfg.getSeverity() == null || cfg.getSeverity().isBlank()) {
            return null;
        }
        try {
            return Severity.valueOf(cfg.getSeverity().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            debugNotes.add("规则配置 severity 无法识别，已使用规则默认 severity。");
            log.warn("规则 {} severity 解析失败: {}", ruleCode, cfg.getSeverity());
            return null;
        }
    }

    private List<SqlRuleDebugTrace> buildDebugRules(List<RuleExecutionSnapshot> snapshots, LintResult result) {
        List<SqlRuleDebugTrace> traces = new ArrayList<>();
        for (RuleExecutionSnapshot snapshot : snapshots) {
            List<LintIssue> issues = snapshot.issues();
            List<String> notes = new ArrayList<>(snapshot.runtimeConfig().debugNotes());
            if (snapshot.status() == SqlRuleMatchStatus.MATCHED || snapshot.status() == SqlRuleMatchStatus.NO_MATCH) {
                notes.add("当前 trace 基于规则执行后的 issue、source range、suppression 和 fixedSql 计划生成。");
            }
            traces.add(SqlRuleDebugTrace.builder()
                    .ruleCode(snapshot.rule().getCode())
                    .ruleName(snapshot.rule().getName())
                    .enabled(snapshot.runtimeConfig().enabled())
                    .severity(dominantSeverity(snapshot.runtimeConfig().overrideSeverity(), issues))
                    .paramsSnapshot(sanitizeParams(snapshot.runtimeConfig().params()))
                    .matchTrace(matchTrace(snapshot))
                    .sourceRange(firstSourceRange(issues))
                    .fixStrategy(fixStrategy(snapshot.rule().getCode(), result))
                    .suppressionStatus(suppressionStatus(issues))
                    .debugNotes(notes)
                    .build());
        }
        return traces;
    }

    private List<SqlRuleMatchTrace> matchTrace(RuleExecutionSnapshot snapshot) {
        if (snapshot.status() == SqlRuleMatchStatus.DISABLED) {
            return List.of(SqlRuleMatchTrace.builder()
                    .status(SqlRuleMatchStatus.DISABLED)
                    .message(snapshot.runtimeConfig().disabledReason())
                    .build());
        }
        if (snapshot.status() == SqlRuleMatchStatus.UNPARSED) {
            return List.of(SqlRuleMatchTrace.builder()
                    .status(SqlRuleMatchStatus.UNPARSED)
                    .message("SQL 未解析到可检查的 CREATE TABLE，规则未执行。")
                    .build());
        }
        if (snapshot.executionError() != null) {
            LintIssue issue = snapshot.issues().isEmpty() ? null : snapshot.issues().get(0);
            return List.of(SqlRuleMatchTrace.builder()
                    .status(SqlRuleMatchStatus.ERROR)
                    .message("规则执行异常，已转换为 warning issue。")
                    .severity(issue == null ? Severity.WARNING : issue.getSeverity())
                    .issueMessage(issue == null ? snapshot.executionError() : issue.getMessage())
                    .sourceRange(toSourceRange(issue))
                    .fixStatus(issue == null ? null : issue.getFixStatus())
                    .fixReasonCode(issue == null ? null : issue.getFixReasonCode())
                    .suppressionId(issue == null ? null : issue.getSuppressionId())
                    .build());
        }
        if (snapshot.issues().isEmpty()) {
            return List.of(SqlRuleMatchTrace.builder()
                    .status(SqlRuleMatchStatus.NO_MATCH)
                    .message("规则已执行，当前 SQL 和参数快照下未产生 issue。")
                    .build());
        }
        List<SqlRuleMatchTrace> traces = new ArrayList<>();
        for (LintIssue issue : snapshot.issues()) {
            traces.add(SqlRuleMatchTrace.builder()
                    .status(SqlRuleMatchStatus.MATCHED)
                    .message("规则命中 lint issue。")
                    .severity(issue.getSeverity())
                    .issueMessage(issue.getMessage())
                    .tableName(issue.getTableName())
                    .columnName(issue.getColumnName())
                    .sourceRange(toSourceRange(issue))
                    .fixStatus(issue.getFixStatus())
                    .fixReasonCode(issue.getFixReasonCode())
                    .suppressionId(issue.getSuppressionId())
                    .build());
        }
        return traces;
    }

    private Severity dominantSeverity(Severity overrideSeverity, List<LintIssue> issues) {
        if (overrideSeverity != null) {
            return overrideSeverity;
        }
        if (issues == null || issues.isEmpty()) {
            return null;
        }
        if (issues.stream().anyMatch(issue -> issue.getSeverity() == Severity.ERROR)) {
            return Severity.ERROR;
        }
        if (issues.stream().anyMatch(issue -> issue.getSeverity() == Severity.WARNING)) {
            return Severity.WARNING;
        }
        if (issues.stream().anyMatch(issue -> issue.getSeverity() == Severity.SUGGESTION)) {
            return Severity.SUGGESTION;
        }
        return issues.get(0).getSeverity();
    }

    private SqlRuleSourceRange firstSourceRange(List<LintIssue> issues) {
        if (issues == null) {
            return null;
        }
        return issues.stream()
                .map(this::toSourceRange)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    private SqlRuleSourceRange toSourceRange(LintIssue issue) {
        if (issue == null) {
            return null;
        }
        boolean hasRange = issue.getLine() != null
                || issue.getColumn() != null
                || issue.getSourceStart() != null
                || issue.getSourceEnd() != null
                || issue.getTableName() != null
                || issue.getColumnName() != null;
        if (!hasRange) {
            return null;
        }
        return SqlRuleSourceRange.builder()
                .line(issue.getLine())
                .column(issue.getColumn())
                .lineEnd(issue.getLineEnd())
                .columnEnd(issue.getColumnEnd())
                .sourceStart(issue.getSourceStart())
                .sourceEnd(issue.getSourceEnd())
                .locationKind(issue.getLocationKind())
                .tableName(issue.getTableName())
                .columnName(issue.getColumnName())
                .build();
    }

    private SqlRuleFixStrategy fixStrategy(String ruleCode, LintResult result) {
        List<FixChange> changes = result.getFixChanges() == null
                ? List.of()
                : result.getFixChanges().stream()
                .filter(change -> Objects.equals(ruleCode, change.getRuleCode()))
                .toList();
        return SqlRuleFixStrategy.builder()
                .fixPolicy(result.getFixPolicy())
                .fixDryRun(result.getFixDryRun())
                .fixSummary(summary(changes))
                .changes(changes)
                .nextActions(fixNextActions(changes, result.getFixDryRun()))
                .build();
    }

    private FixPlanSummary summary(List<FixChange> changes) {
        int applied = 0;
        int planned = 0;
        int skipped = 0;
        for (FixChange change : changes) {
            if (change.getStatus() == FixChangeStatus.APPLIED) {
                applied += 1;
            } else if (change.getStatus() == FixChangeStatus.PLANNED) {
                planned += 1;
            } else if (change.getStatus() == FixChangeStatus.SKIPPED) {
                skipped += 1;
            }
        }
        return FixPlanSummary.builder()
                .availableCount(changes.size())
                .appliedCount(applied)
                .plannedCount(planned)
                .skippedCount(skipped)
                .build();
    }

    private List<String> fixNextActions(List<FixChange> changes, Boolean dryRun) {
        if (changes == null || changes.isEmpty()) {
            return List.of("当前规则没有可自动生成 fixedSql 的确定性修复项。");
        }
        List<String> actions = new ArrayList<>();
        if (Boolean.TRUE.equals(dryRun)) {
            actions.add("当前为 dry-run 预览，应用 fixedSql 前请人工确认 diff 与风险。");
        }
        if (changes.stream().anyMatch(change -> change.getStatus() == FixChangeStatus.SKIPPED)) {
            actions.add("存在跳过的修复项，请查看 reasonCode 和 explain 后决定是否调整策略或人工处理。");
        }
        if (actions.isEmpty()) {
            actions.add("请结合 sourceRange、diff 和风险等级确认该规则的 fixedSql 结果。");
        }
        return actions;
    }

    private SqlRuleSuppressionStatus suppressionStatus(List<LintIssue> issues) {
        List<LintIssue> safeIssues = issues == null ? List.of() : issues;
        List<LintIssue> suppressed = safeIssues.stream()
                .filter(issue -> Boolean.TRUE.equals(issue.getSuppressed()))
                .toList();
        int activeCount = safeIssues.size() - suppressed.size();
        List<Long> suppressionIds = suppressed.stream()
                .map(LintIssue::getSuppressionId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        List<String> reasons = suppressed.stream()
                .map(LintIssue::getSuppressionReason)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        String summary = suppressed.isEmpty()
                ? "当前规则没有命中豁免。"
                : String.format("当前规则有 %d 个 issue 被豁免抑制。", suppressed.size());
        if (safeIssues.isEmpty()) {
            summary = "当前规则未产生 issue。";
        }
        return SqlRuleSuppressionStatus.builder()
                .activeIssueCount(activeCount)
                .suppressedIssueCount(suppressed.size())
                .suppressionIds(suppressionIds)
                .suppressionReasons(reasons)
                .summary(summary)
                .build();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> sanitizeParams(Map<String, Object> params) {
        if (params == null || params.isEmpty()) {
            return Collections.emptyMap();
        }
        return (Map<String, Object>) SensitiveDataSanitizer.sanitizeValue(params);
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

    private record LintExecution(LintResult result, List<SqlRuleDebugTrace> debugRules) {
    }

    private record RuleSettings(boolean hasProjectRuleConfig, Map<String, RuleConfig> configs) {
    }

    private record RuleRuntimeConfig(
            RuleConfig config,
            boolean enabled,
            Map<String, Object> params,
            Severity overrideSeverity,
            List<String> debugNotes,
            String disabledReason
    ) {
    }

    private record RuleExecutionSnapshot(
            LintRule rule,
            RuleRuntimeConfig runtimeConfig,
            List<LintIssue> issues,
            String executionError,
            SqlRuleMatchStatus status
    ) {
        private static RuleExecutionSnapshot disabled(LintRule rule, RuleRuntimeConfig runtimeConfig) {
            return new RuleExecutionSnapshot(rule, runtimeConfig, List.of(), null, SqlRuleMatchStatus.DISABLED);
        }

        private static RuleExecutionSnapshot unparsed(LintRule rule, RuleRuntimeConfig runtimeConfig) {
            return new RuleExecutionSnapshot(rule, runtimeConfig, List.of(), null, SqlRuleMatchStatus.UNPARSED);
        }

        private static RuleExecutionSnapshot executed(
                LintRule rule,
                RuleRuntimeConfig runtimeConfig,
                List<LintIssue> issues,
                String executionError) {
            List<LintIssue> safeIssues = issues == null ? List.of() : issues;
            SqlRuleMatchStatus status = safeIssues.isEmpty()
                    ? SqlRuleMatchStatus.NO_MATCH
                    : SqlRuleMatchStatus.MATCHED;
            return new RuleExecutionSnapshot(rule, runtimeConfig, safeIssues, executionError, status);
        }
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
