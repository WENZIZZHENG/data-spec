package com.dataspec.aibatch.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.dataspec.aibatch.entity.AiBatchRun;
import com.dataspec.aibatch.model.AiBatchDeliveryPackage;
import com.dataspec.aibatch.model.AiBatchEvidence;
import com.dataspec.aibatch.model.AiBatchFixedSqlSummary;
import com.dataspec.aibatch.model.AiBatchIssueRuleSummary;
import com.dataspec.aibatch.model.AiBatchIssueSummary;
import com.dataspec.aibatch.model.AiBatchItemResult;
import com.dataspec.aibatch.model.AiBatchRunDetail;
import com.dataspec.aibatch.model.AiBatchSqlLintItemReq;
import com.dataspec.aibatch.model.AiBatchSqlLintReq;
import com.dataspec.aibatch.model.AiBatchSummary;
import com.dataspec.aibatch.repository.AiBatchRunRepository;
import com.dataspec.aibatch.service.AiBatchService;
import com.dataspec.common.exception.BizException;
import com.dataspec.dialect.model.DialectDiagnostic;
import com.dataspec.lint.engine.SqlLintService;
import com.dataspec.lint.model.LintIssue;
import com.dataspec.lint.model.LintResult;
import com.dataspec.security.context.DataSpecSecurityContext;
import com.dataspec.security.context.ProjectAccessGuard;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 同步 AI 批量任务服务。第一版只编排已有 SQL lint 能力，不引入后台队列。
 */
@Service
@RequiredArgsConstructor
public class AiBatchServiceImpl implements AiBatchService {

    private static final String PACKAGE_VERSION = "ai-batch-delivery@1";
    private static final String BATCH_TYPE_SQL_LINT = "SQL_LINT";
    private static final Pattern JDBC_URL = Pattern.compile("jdbc:[^\\s\"'<>]+", Pattern.CASE_INSENSITIVE);
    private static final Pattern PASSWORD = Pattern.compile("(?i)(password|pwd)\\s*=\\s*(['\"]?)[^\\s\"';&]+\\2");
    private static final Pattern TOKEN_VALUE = Pattern.compile("(?i)(token|api[_-]?token)\\s*=\\s*(['\"]?)[^\\s\"';&]+\\2");
    private static final Pattern TOKEN = Pattern.compile("(?i)(bearer\\s+)[A-Za-z0-9._\\-]+");

    private final AiBatchRunRepository aiBatchRunRepository;
    private final SqlLintService sqlLintService;
    private final ObjectMapper objectMapper;

    @Override
    public AiBatchDeliveryPackage createSqlLintBatch(AiBatchSqlLintReq req) {
        validateReq(req);
        ProjectAccessGuard.requireProjectAccess(req.projectId());

        List<AiBatchItemResult> itemResults = new ArrayList<>();
        for (AiBatchSqlLintItemReq item : req.items()) {
            itemResults.add(runLintItem(req.projectId(), item));
        }

        AiBatchSummary summary = buildSummary(itemResults);
        AiBatchIssueSummary issueSummary = buildIssueSummary(itemResults);
        AiBatchFixedSqlSummary fixedSqlSummary = buildFixedSqlSummary(itemResults);
        String status = resolveStatus(summary);
        String source = normalizeSource(req.source());

        AiBatchRun run = new AiBatchRun();
        run.setProjectId(req.projectId());
        run.setBatchType(BATCH_TYPE_SQL_LINT);
        run.setSource(source);
        run.setStatus(status);
        run.setSummaryJson(writeJson(summary));
        run.setPayloadJson("{}");
        run.setOperatorName(DataSpecSecurityContext.currentOperator());
        aiBatchRunRepository.insert(run);

        LocalDateTime createdAt = run.getCreatedAt() == null ? LocalDateTime.now() : run.getCreatedAt();
        AiBatchDeliveryPackage deliveryPackage = new AiBatchDeliveryPackage(
                PACKAGE_VERSION,
                batchId(run),
                req.projectId(),
                BATCH_TYPE_SQL_LINT,
                source,
                status,
                summary,
                itemResults,
                issueSummary,
                fixedSqlSummary,
                List.of(),
                buildEvidence(source, summary),
                buildNextActions(summary),
                createdAt
        );
        run.setPayloadJson(writeJson(deliveryPackage));
        run.setSummaryJson(writeJson(summary));
        aiBatchRunRepository.update(run);
        return deliveryPackage;
    }

    @Override
    public IPage<AiBatchRun> listByProject(Long projectId, int current, int size) {
        if (projectId == null) {
            throw new BizException("项目ID不能为空");
        }
        ProjectAccessGuard.requireProjectAccess(projectId);
        return aiBatchRunRepository.findByProjectId(projectId, current, size);
    }

    @Override
    public AiBatchRunDetail getDetail(Long id) {
        if (id == null) {
            throw new BizException("AI 批量任务 ID 不能为空");
        }
        AiBatchRun run = aiBatchRunRepository.findById(id)
                .orElseThrow(() -> new BizException("AI 批量任务不存在: " + id));
        ProjectAccessGuard.requireProjectAccess(run.getProjectId());
        return new AiBatchRunDetail(run, readPackage(run.getPayloadJson()));
    }

    @Override
    public AiBatchDeliveryPackage getPackage(Long id) {
        return getDetail(id).deliveryPackage();
    }

    private void validateReq(AiBatchSqlLintReq req) {
        if (req == null || req.projectId() == null) {
            throw new BizException("项目ID不能为空");
        }
        if (req.items() == null || req.items().isEmpty()) {
            throw new BizException("批量 SQL 项不能为空");
        }
    }

    private AiBatchItemResult runLintItem(Long projectId, AiBatchSqlLintItemReq item) {
        String itemName = sanitize(item == null ? null : item.itemName());
        String filePath = sanitize(item == null ? null : item.filePath());
        String sql = item == null ? null : item.sql();
        if (sql == null || sql.isBlank()) {
            return failedItem(itemName, filePath, "SQL 不能为空");
        }
        try {
            LintResult result = sqlLintService.lint(sql, projectId);
            List<LintIssue> issues = result.getIssues() == null
                    ? List.of()
                    : result.getIssues().stream().map(this::sanitizeIssue).toList();
            return new AiBatchItemResult(
                    itemName,
                    filePath,
                    "SUCCESS",
                    result.getErrorCount(),
                    result.getWarningCount(),
                    result.getSuggestionCount(),
                    result.getSuppressedCount(),
                    !isBlank(result.getFixedSql()),
                    sanitize(result.getFixedSql()),
                    sanitize(result.getFixedSqlDiff()),
                    issues,
                    sanitizeDiagnostics(result.getDialectDiagnostics()),
                    null,
                    null
            );
        } catch (Exception e) {
            return failedItem(itemName, filePath, e.getMessage());
        }
    }

    private AiBatchItemResult failedItem(String itemName, String filePath, String message) {
        return new AiBatchItemResult(
                itemName,
                filePath,
                "FAILED",
                0,
                0,
                0,
                0,
                false,
                null,
                null,
                List.of(),
                List.of(),
                null,
                sanitize(message)
        );
    }

    private AiBatchSummary buildSummary(List<AiBatchItemResult> items) {
        int success = (int) items.stream().filter(item -> "SUCCESS".equals(item.status())).count();
        int failed = items.size() - success;
        int errors = items.stream().mapToInt(AiBatchItemResult::errorCount).sum();
        int warnings = items.stream().mapToInt(AiBatchItemResult::warningCount).sum();
        int suggestions = items.stream().mapToInt(AiBatchItemResult::suggestionCount).sum();
        int fixedSql = (int) items.stream().filter(AiBatchItemResult::fixedSqlAvailable).count();
        return new AiBatchSummary(items.size(), success, failed, errors, warnings, suggestions, fixedSql);
    }

    private AiBatchIssueSummary buildIssueSummary(List<AiBatchItemResult> items) {
        Map<String, RuleCounter> counters = new LinkedHashMap<>();
        for (AiBatchItemResult item : items) {
            for (LintIssue issue : item.issues()) {
                String key = isBlank(issue.getRuleCode()) ? "unknown" : issue.getRuleCode();
                RuleCounter counter = counters.computeIfAbsent(key, ignored -> new RuleCounter(issue.getRuleCode(), issue.getRuleName()));
                counter.count++;
            }
        }
        List<AiBatchIssueRuleSummary> byRule = counters.values().stream()
                .sorted(Comparator.comparingInt((RuleCounter counter) -> counter.count).reversed())
                .map(counter -> new AiBatchIssueRuleSummary(counter.ruleCode, counter.ruleName, counter.count))
                .toList();
        int errors = items.stream().mapToInt(AiBatchItemResult::errorCount).sum();
        int warnings = items.stream().mapToInt(AiBatchItemResult::warningCount).sum();
        int suggestions = items.stream().mapToInt(AiBatchItemResult::suggestionCount).sum();
        return new AiBatchIssueSummary(errors, warnings, suggestions, byRule);
    }

    private AiBatchFixedSqlSummary buildFixedSqlSummary(List<AiBatchItemResult> items) {
        int available = (int) items.stream().filter(AiBatchItemResult::fixedSqlAvailable).count();
        int changed = (int) items.stream()
                .filter(item -> item.fixedSqlAvailable() && !isBlank(item.fixedSqlDiff()))
                .count();
        return new AiBatchFixedSqlSummary(available, changed);
    }

    private String resolveStatus(AiBatchSummary summary) {
        if (summary.failedItems() == 0) {
            return "SUCCESS";
        }
        if (summary.successItems() == 0) {
            return "FAILED";
        }
        return "PARTIAL_FAILED";
    }

    private List<AiBatchEvidence> buildEvidence(String source, AiBatchSummary summary) {
        return List.of(
                new AiBatchEvidence("batchType", "任务类型", BATCH_TYPE_SQL_LINT),
                new AiBatchEvidence("source", "任务来源", source),
                new AiBatchEvidence("summary", "SQL 项数量", String.valueOf(summary.totalItems()))
        );
    }

    private List<String> buildNextActions(AiBatchSummary summary) {
        List<String> actions = new ArrayList<>();
        if (summary.failedItems() > 0) {
            actions.add("查看失败项 errorMessage，修正输入后缩小范围重试");
        }
        if (summary.errorCount() > 0) {
            actions.add("优先修复 ERROR 级 SQL 标准问题");
        }
        if (summary.fixedSqlCount() > 0) {
            actions.add("人工确认 fixedSql 后再应用到业务仓库");
        }
        if (actions.isEmpty()) {
            actions.add("无需处理");
        }
        return actions;
    }

    private String batchId(AiBatchRun run) {
        return run.getId() == null ? "server-pending" : "server-" + run.getId();
    }

    private String normalizeSource(String source) {
        String sanitized = sanitize(source);
        if (isBlank(sanitized)) {
            return "api";
        }
        String normalized = sanitized.trim();
        return normalized.length() <= 200 ? normalized : normalized.substring(0, 200);
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new BizException("AI 批量任务 JSON 序列化失败: " + e.getMessage());
        }
    }

    private AiBatchDeliveryPackage readPackage(String json) {
        if (isBlank(json)) {
            throw new BizException("AI 批量任务交付包为空");
        }
        try {
            return objectMapper.readValue(json, AiBatchDeliveryPackage.class);
        } catch (Exception e) {
            throw new BizException("AI 批量任务交付包解析失败: " + e.getMessage());
        }
    }

    private List<DialectDiagnostic> sanitizeDiagnostics(List<DialectDiagnostic> diagnostics) {
        if (diagnostics == null || diagnostics.isEmpty()) {
            return List.of();
        }
        return diagnostics.stream()
                .map(diagnostic -> DialectDiagnostic.of(
                        sanitize(diagnostic.dialect()),
                        diagnostic.capability(),
                        diagnostic.level(),
                        sanitize(diagnostic.code()),
                        sanitize(diagnostic.message()),
                        sanitize(diagnostic.nextAction())
                ))
                .toList();
    }

    private LintIssue sanitizeIssue(LintIssue issue) {
        return LintIssue.builder()
                .severity(issue.getSeverity())
                .ruleCode(sanitize(issue.getRuleCode()))
                .ruleName(sanitize(issue.getRuleName()))
                .message(sanitize(issue.getMessage()))
                .tableName(sanitize(issue.getTableName()))
                .columnName(sanitize(issue.getColumnName()))
                .suggestion(sanitize(issue.getSuggestion()))
                .replacement(sanitize(issue.getReplacement()))
                .before(sanitize(issue.getBefore()))
                .after(sanitize(issue.getAfter()))
                .confidence(issue.getConfidence())
                .line(issue.getLine())
                .column(issue.getColumn())
                .lineEnd(issue.getLineEnd())
                .columnEnd(issue.getColumnEnd())
                .sourceStart(issue.getSourceStart())
                .sourceEnd(issue.getSourceEnd())
                .locationKind(sanitize(issue.getLocationKind()))
                .suppressed(issue.getSuppressed())
                .suppressionId(issue.getSuppressionId())
                .suppressionReason(sanitize(issue.getSuppressionReason()))
                .build();
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

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static final class RuleCounter {
        private final String ruleCode;
        private final String ruleName;
        private int count;

        private RuleCounter(String ruleCode, String ruleName) {
            this.ruleCode = ruleCode;
            this.ruleName = ruleName;
        }
    }
}
