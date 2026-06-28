package com.dataspec.aifeedback.service.impl;

import com.dataspec.aifeedback.model.AiFeedbackAction;
import com.dataspec.aifeedback.model.AiFeedbackEvidence;
import com.dataspec.aifeedback.model.AiFeedbackReport;
import com.dataspec.aifeedback.model.AiFeedbackSampleSize;
import com.dataspec.aifeedback.model.AiFeedbackSignal;
import com.dataspec.aifeedback.model.AiFeedbackSummary;
import com.dataspec.aifeedback.service.AiFeedbackService;
import com.dataspec.aireplay.entity.AiJobRecord;
import com.dataspec.aireplay.repository.AiJobRecordRepository;
import com.dataspec.common.exception.BizException;
import com.dataspec.field.entity.Field;
import com.dataspec.field.repository.FieldRepository;
import com.dataspec.lint.entity.SqlCheckRecord;
import com.dataspec.lint.model.LintIssue;
import com.dataspec.lint.repository.SqlCheckRecordRepository;
import com.dataspec.reverseimport.entity.FieldSource;
import com.dataspec.reverseimport.repository.FieldSourceRepository;
import com.dataspec.ruleexemption.entity.RuleExemption;
import com.dataspec.ruleexemption.repository.RuleExemptionRepository;
import com.dataspec.security.context.ProjectAccessGuard;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 基于既有记录生成只读反馈报告。第一版不新增行为埋点，避免把个人工具变成监控系统。
 */
@Service
@RequiredArgsConstructor
public class AiFeedbackServiceImpl implements AiFeedbackService {

    private static final int SAMPLE_LIMIT = 100;
    private static final Pattern JDBC_URL = Pattern.compile("jdbc:[^\\s\"'<>]+", Pattern.CASE_INSENSITIVE);
    private static final Pattern PASSWORD = Pattern.compile("(?i)(password|pwd)\\s*=\\s*(['\"]?)[^\\s\"';&]+\\2");
    private static final Pattern TOKEN_VALUE = Pattern.compile("(?i)(token|api[_-]?token)\\s*=\\s*(['\"]?)[^\\s\"';&]+\\2");
    private static final Pattern TOKEN = Pattern.compile("(?i)(bearer\\s+)[A-Za-z0-9._\\-]+");

    private final AiJobRecordRepository aiJobRecordRepository;
    private final SqlCheckRecordRepository sqlCheckRecordRepository;
    private final RuleExemptionRepository ruleExemptionRepository;
    private final FieldSourceRepository fieldSourceRepository;
    private final FieldRepository fieldRepository;
    private final ObjectMapper objectMapper;

    @Override
    public AiFeedbackReport buildReport(Long projectId) {
        if (projectId == null) {
            throw new BizException("项目ID不能为空");
        }
        ProjectAccessGuard.requireProjectAccess(projectId);

        List<Field> fields = fieldRepository.findAllByProjectId(projectId);
        List<AiJobRecord> aiJobs = aiJobRecordRepository.findRecentByProjectId(projectId, SAMPLE_LIMIT);
        List<SqlCheckRecord> sqlChecks = sqlCheckRecordRepository.findRecentByProjectId(projectId, SAMPLE_LIMIT);
        List<RuleExemption> ruleExemptions = ruleExemptionRepository.findByProjectId(projectId);
        List<FieldSource> fieldSources = fieldSourceRepository.findByProjectId(projectId);

        Map<String, SignalCounter> fieldCounters = new LinkedHashMap<>();
        Map<String, SignalCounter> ruleCounters = new LinkedHashMap<>();
        List<AiFeedbackSignal> fixedSqlSignals = buildFixedSqlSignals(sqlChecks);
        List<AiFeedbackSignal> unmanagedSignals = buildUnmanagedSignals(fieldSources, sqlChecks);

        collectFieldSignals(fields, aiJobs, sqlChecks, fieldCounters);
        collectRuleSignals(sqlChecks, ruleExemptions, ruleCounters);

        List<AiFeedbackSignal> fieldSignals = toSignals(fieldCounters, "field_reference", "info", "补充字段别名、注释或示例", "/fields?keyword=");
        List<AiFeedbackSignal> ruleSignals = toSignals(ruleCounters, "rule_issue", "warning", "查看规则配置或规则例外", "/rule-exemptions?ruleCode=");
        List<AiFeedbackAction> nextActions = buildNextActions(fieldSignals, ruleSignals, fixedSqlSignals, unmanagedSignals);
        AiFeedbackSummary summary = new AiFeedbackSummary(
                aiJobs.size(),
                sqlChecks.size(),
                ruleExemptions.size(),
                fieldSources.size(),
                fieldSignals.size(),
                ruleSignals.size(),
                (int) sqlChecks.stream().filter(record -> !isBlank(record.getFixedSql())).count(),
                true,
                "字段推荐暂未持久化逐次命中/未命中事件，本报告只展示可由现有记录解释的反馈信号。"
        );
        AiFeedbackSampleSize sampleSize = new AiFeedbackSampleSize(
                aiJobs.size(),
                sqlChecks.size(),
                ruleExemptions.size(),
                fieldSources.size(),
                fields.size()
        );
        return new AiFeedbackReport(
                projectId,
                summary,
                fieldSignals,
                ruleSignals,
                fixedSqlSignals,
                unmanagedSignals,
                nextActions,
                sampleSize,
                LocalDateTime.now()
        );
    }

    private void collectFieldSignals(
            List<Field> fields,
            List<AiJobRecord> aiJobs,
            List<SqlCheckRecord> sqlChecks,
            Map<String, SignalCounter> counters
    ) {
        for (Field field : fields) {
            Set<String> terms = fieldTerms(field);
            if (terms.isEmpty()) {
                continue;
            }
            for (AiJobRecord job : aiJobs) {
                String text = normalizeText(job.getTitle(), job.getInputSummary(), job.getInputPayloadJson(), job.getOutputPayloadJson());
                if (containsAny(text, terms)) {
                    counter(counters, field.getName(), field.getName())
                            .add(1, new AiFeedbackEvidence("AI_JOB", job.getId(), sanitize("AI 作业引用字段 " + field.getName())));
                }
            }
            for (SqlCheckRecord record : sqlChecks) {
                if (containsAny(normalizeText(record.getFixedSql()), terms)) {
                    counter(counters, field.getName(), field.getName())
                            .add(1, new AiFeedbackEvidence("SQL_CHECK", record.getId(), sanitize("fixedSql 引用字段 " + field.getName())));
                }
                for (LintIssue issue : parseIssues(record)) {
                    String text = normalizeText(issue.getColumnName(), issue.getReplacement(), issue.getBefore(), issue.getAfter(), issue.getSuggestion());
                    if (containsAny(text, terms)) {
                        counter(counters, field.getName(), field.getName())
                                .add(1, new AiFeedbackEvidence("SQL_ISSUE", record.getId(), sanitize("SQL 问题关联字段 " + field.getName())));
                    }
                }
            }
        }
    }

    private void collectRuleSignals(
            List<SqlCheckRecord> sqlChecks,
            List<RuleExemption> ruleExemptions,
            Map<String, SignalCounter> counters
    ) {
        for (SqlCheckRecord record : sqlChecks) {
            for (LintIssue issue : parseIssues(record)) {
                String ruleCode = isBlank(issue.getRuleCode()) ? "unknown" : issue.getRuleCode();
                counter(counters, ruleCode, ruleCode)
                        .add(1, new AiFeedbackEvidence("SQL_ISSUE", record.getId(), sanitize("规则问题: " + nullToEmpty(issue.getRuleName()))));
            }
        }
        for (RuleExemption exemption : ruleExemptions) {
            String ruleCode = isBlank(exemption.getRuleCode()) ? "unknown" : exemption.getRuleCode();
            counter(counters, ruleCode, ruleCode)
                    .add(1, new AiFeedbackEvidence("RULE_EXEMPTION", exemption.getId(), sanitize("规则例外: " + nullToEmpty(exemption.getReason()))));
        }
    }

    private List<AiFeedbackSignal> buildFixedSqlSignals(List<SqlCheckRecord> records) {
        long count = records.stream().filter(record -> !isBlank(record.getFixedSql())).count();
        if (count == 0) {
            return List.of();
        }
        return List.of(new AiFeedbackSignal(
                "fixed_sql",
                "fixedSql 可用记录 " + count + " 条",
                (int) count,
                "warning",
                records.stream()
                        .filter(record -> !isBlank(record.getFixedSql()))
                        .limit(5)
                        .map(record -> new AiFeedbackEvidence("SQL_CHECK", record.getId(), "SQL 检查记录存在 fixedSql"))
                        .toList(),
                "人工确认 fixedSql 后，再回到 SQL 校验记录或业务仓库应用修改",
                "/sql-lint"
        ));
    }

    private List<AiFeedbackSignal> buildUnmanagedSignals(List<FieldSource> fieldSources, List<SqlCheckRecord> sqlChecks) {
        List<AiFeedbackSignal> signals = new ArrayList<>();
        if (!fieldSources.isEmpty()) {
            signals.add(new AiFeedbackSignal(
                    "standardized_from_reverse_import",
                    "反向导入已转正 " + fieldSources.size() + " 个字段",
                    fieldSources.size(),
                    "info",
                    fieldSources.stream()
                            .limit(5)
                            .map(source -> new AiFeedbackEvidence("FIELD_SOURCE", source.getId(), sanitize("来源 " + nullToEmpty(source.getTableName()) + "." + nullToEmpty(source.getColumnName()))))
                            .toList(),
                    "复查这些字段是否需要补别名、注释或示例",
                    "/fields"
            ));
        }
        int namingIssues = 0;
        Long firstNamingIssueRecordId = null;
        for (SqlCheckRecord record : sqlChecks) {
            for (LintIssue issue : parseIssues(record)) {
                String ruleCode = nullToEmpty(issue.getRuleCode()).toLowerCase(Locale.ROOT);
                if (ruleCode.contains("field") || ruleCode.contains("naming") || ruleCode.contains("recommended")) {
                    namingIssues++;
                    if (firstNamingIssueRecordId == null) {
                        firstNamingIssueRecordId = record.getId();
                    }
                }
            }
        }
        if (namingIssues > 0) {
            signals.add(new AiFeedbackSignal(
                    "unmanaged_or_naming_gap",
                    "字段命名/推荐类问题 " + namingIssues + " 个",
                    namingIssues,
                    "warning",
                    List.of(new AiFeedbackEvidence("SQL_CHECK", firstNamingIssueRecordId, "SQL 检查记录中存在字段标准化信号")),
                    "优先处理高频命名问题，必要时补充标准字段或别名",
                    "/field-quality"
            ));
        }
        return signals;
    }

    private List<AiFeedbackSignal> toSignals(
            Map<String, SignalCounter> counters,
            String signalType,
            String defaultSeverity,
            String suggestedAction,
            String routePrefix
    ) {
        return counters.values().stream()
                .sorted(Comparator.comparingInt((SignalCounter counter) -> counter.count).reversed().thenComparing(counter -> counter.title))
                .limit(10)
                .map(counter -> new AiFeedbackSignal(
                        signalType,
                        counter.title + " 出现 " + counter.count + " 次",
                        counter.count,
                        counter.count >= 3 ? "warning" : defaultSeverity,
                        List.copyOf(counter.evidence),
                        suggestedAction,
                        routePrefix + encode(counter.key)
                ))
                .toList();
    }

    private List<AiFeedbackAction> buildNextActions(
            List<AiFeedbackSignal> fieldSignals,
            List<AiFeedbackSignal> ruleSignals,
            List<AiFeedbackSignal> fixedSqlSignals,
            List<AiFeedbackSignal> unmanagedSignals
    ) {
        List<AiFeedbackAction> actions = new ArrayList<>();
        if (!fieldSignals.isEmpty()) {
            actions.add(new AiFeedbackAction("维护高频字段", "检查高频字段是否需要补别名、注释、示例或状态", "HIGH", fieldSignals.get(0).targetRoute()));
        }
        if (!ruleSignals.isEmpty()) {
            actions.add(new AiFeedbackAction("处理高频规则问题", "先判断是标准问题、SQL 问题还是需要规则例外", "HIGH", ruleSignals.get(0).targetRoute()));
        }
        if (!fixedSqlSignals.isEmpty()) {
            actions.add(new AiFeedbackAction("复核 fixedSql", "人工确认可修复 SQL，再决定是否应用到业务仓库", "MEDIUM", fixedSqlSignals.get(0).targetRoute()));
        }
        if (!unmanagedSignals.isEmpty()) {
            actions.add(new AiFeedbackAction("收敛未纳管信号", "从反向导入和字段质量页补齐标准字段", "MEDIUM", unmanagedSignals.get(0).targetRoute()));
        }
        if (actions.isEmpty()) {
            actions.add(new AiFeedbackAction("继续积累反馈", "运行 SQL 校验、AI 回放或反向导入后再查看反馈报告", "LOW", "/ai-replay"));
        }
        return actions;
    }

    private List<LintIssue> parseIssues(SqlCheckRecord record) {
        if (record == null || isBlank(record.getIssuesJson())) {
            return List.of();
        }
        try {
            return objectMapper.readValue(record.getIssuesJson(), new TypeReference<>() {
            });
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private Set<String> fieldTerms(Field field) {
        Set<String> terms = new LinkedHashSet<>();
        addTerm(terms, field.getName());
        addTerm(terms, field.getDisplayName());
        if (!isBlank(field.getAliases())) {
            for (String alias : field.getAliases().split(",")) {
                addTerm(terms, alias);
            }
        }
        return terms;
    }

    private void addTerm(Set<String> terms, String value) {
        if (!isBlank(value)) {
            String term = value.trim().toLowerCase(Locale.ROOT);
            // 过滤 id/no 等短词，避免在任意 SQL 或 payload 片段中产生大量误报信号。
            if (term.length() >= 3) {
                terms.add(term);
            }
        }
    }

    private boolean containsAny(String text, Set<String> terms) {
        if (isBlank(text)) {
            return false;
        }
        String normalized = text.toLowerCase(Locale.ROOT);
        return terms.stream().anyMatch(normalized::contains);
    }

    private SignalCounter counter(Map<String, SignalCounter> counters, String key, String title) {
        return counters.computeIfAbsent(key, ignored -> new SignalCounter(key, title));
    }

    private String normalizeText(String... values) {
        StringBuilder builder = new StringBuilder();
        for (String value : values) {
            if (!isBlank(value)) {
                builder.append(' ').append(value);
            }
        }
        return builder.toString();
    }

    private String sanitize(String value) {
        if (value == null) {
            return null;
        }
        String sanitized = JDBC_URL.matcher(value).replaceAll("jdbc:***");
        sanitized = PASSWORD.matcher(sanitized).replaceAll("$1=***");
        sanitized = TOKEN_VALUE.matcher(sanitized).replaceAll("$1=***");
        sanitized = TOKEN.matcher(sanitized).replaceAll("$1***");
        return sanitized;
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static final class SignalCounter {
        private final String key;
        private final String title;
        private final List<AiFeedbackEvidence> evidence = new ArrayList<>();
        private int count;

        private SignalCounter(String key, String title) {
            this.key = key;
            this.title = title;
        }

        private void add(int delta, AiFeedbackEvidence item) {
            count += delta;
            if (evidence.size() < 5) {
                evidence.add(item);
            }
        }
    }
}
