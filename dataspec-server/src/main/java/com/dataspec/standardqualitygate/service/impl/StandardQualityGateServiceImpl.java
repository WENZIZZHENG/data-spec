package com.dataspec.standardqualitygate.service.impl;

import com.dataspec.common.exception.BizException;
import com.dataspec.common.sanitize.SensitiveDataSanitizer;
import com.dataspec.fieldquality.model.FieldQualityIssue;
import com.dataspec.fieldquality.model.FieldQualityReport;
import com.dataspec.fieldquality.model.FieldQualitySummary;
import com.dataspec.fieldquality.service.FieldQualityService;
import com.dataspec.security.context.ProjectAccessGuard;
import com.dataspec.standardhealth.entity.StandardHealthSnapshot;
import com.dataspec.standardhealth.model.StandardHealthCoverageInput;
import com.dataspec.standardhealth.repository.StandardHealthSnapshotRepository;
import com.dataspec.standardqualitygate.entity.StandardQualityGate;
import com.dataspec.standardqualitygate.model.QualityGateCheckResult;
import com.dataspec.standardqualitygate.model.QualityGateLintSummary;
import com.dataspec.standardqualitygate.model.QualityGateSummary;
import com.dataspec.standardqualitygate.model.StandardQualityGateConfig;
import com.dataspec.standardqualitygate.model.StandardQualityGateEvaluateReq;
import com.dataspec.standardqualitygate.model.StandardQualityGateResult;
import com.dataspec.standardqualitygate.model.StandardQualityGateSaveReq;
import com.dataspec.standardqualitygate.repository.StandardQualityGateRepository;
import com.dataspec.standardqualitygate.service.StandardQualityGateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

/**
 * 标准质量门禁服务。门禁结果只用于显式 check，不在字段保存链路里隐式拦截。
 */
@Service
@RequiredArgsConstructor
public class StandardQualityGateServiceImpl implements StandardQualityGateService {

    private static final int DEFAULT_MIN_COVERAGE = 80;
    private static final int DEFAULT_MIN_AVERAGE_FIELD_SCORE = 80;
    private static final int DEFAULT_MAX_ERROR_ISSUES = 0;
    private static final int DEFAULT_MAX_NEW_UNMANAGED_FIELDS = 0;

    private final StandardQualityGateRepository standardQualityGateRepository;
    private final FieldQualityService fieldQualityService;
    private final StandardHealthSnapshotRepository standardHealthSnapshotRepository;

    @Override
    public StandardQualityGateConfig getConfig(Long projectId) {
        requireProject(projectId);
        return toConfig(loadOrDefault(projectId));
    }

    @Override
    @Transactional
    public StandardQualityGateConfig saveConfig(StandardQualityGateSaveReq req) {
        Long projectId = req == null ? null : req.getProjectId();
        requireProject(projectId);
        validate(req);

        StandardQualityGate gate = standardQualityGateRepository.findByProjectId(projectId)
                .orElseGet(() -> defaultGate(projectId));
        gate.setEnabled(Boolean.TRUE.equals(req.getEnabled()));
        gate.setMinCoverage(valueOrDefault(req.getMinCoverage(), DEFAULT_MIN_COVERAGE));
        gate.setMinAverageFieldScore(valueOrDefault(req.getMinAverageFieldScore(), DEFAULT_MIN_AVERAGE_FIELD_SCORE));
        gate.setMaxErrorIssues(valueOrDefault(req.getMaxErrorIssues(), DEFAULT_MAX_ERROR_ISSUES));
        gate.setMaxNewUnmanagedFields(valueOrDefault(req.getMaxNewUnmanagedFields(), DEFAULT_MAX_NEW_UNMANAGED_FIELDS));
        gate.setRequiredSensitiveMarking(req.getRequiredSensitiveMarking() == null || req.getRequiredSensitiveMarking());
        gate.setConfigJson(null);

        if (gate.getId() == null) {
            standardQualityGateRepository.insert(gate);
        } else {
            standardQualityGateRepository.update(gate);
        }
        return toConfig(gate);
    }

    @Override
    public StandardQualityGateResult evaluate(StandardQualityGateEvaluateReq req) {
        Long projectId = req == null ? null : req.getProjectId();
        requireProject(projectId);
        validateEvaluateInput(req);

        StandardQualityGate gate = loadOrDefault(projectId);
        StandardQualityGateResult result = new StandardQualityGateResult();
        result.setProjectId(projectId);
        result.setEnabled(Boolean.TRUE.equals(gate.getEnabled()));
        result.setConfig(toConfig(gate));
        result.setEvaluatedAt(LocalDateTime.now());

        if (!Boolean.TRUE.equals(gate.getEnabled())) {
            result.setStatus("DISABLED");
            result.setNextActions(List.of("启用质量门禁后再执行 CI/AI 检查"));
            return result;
        }

        FieldQualityReport qualityReport = fieldQualityService.report(projectId);
        FieldQualitySummary quality = qualityReport.getSummary();
        StandardHealthCoverageInput coverage = resolveCoverage(projectId, req == null ? null : req.getCoverage()).orElse(null);
        QualityGateLintSummary lintSummary = req == null ? null : req.getLintSummary();

        List<QualityGateCheckResult> checks = new ArrayList<>();
        checks.add(minCheck(
                "average_field_score",
                "字段质量均分",
                (double) quality.getAverageScore(),
                gate.getMinAverageFieldScore(),
                "/field-quality",
                "补齐低分字段的注释、别名、示例、分类和格式约束"
        ));
        checks.add(maxCheck(
                "field_error_issues",
                "字段 ERROR 问题数",
                (double) quality.getErrorIssueCount(),
                gate.getMaxErrorIssues(),
                "/field-quality",
                "优先处理 ERROR 级字段质量问题"
        ));
        checks.add(sensitiveCheck(gate, sensitiveGapCount(qualityReport)));
        checks.add(coverageCheck(gate, coverage));
        checks.add(unmanagedCheck(gate, coverage));
        checks.add(lintCheck(gate, lintSummary));

        result.setChecks(checks);
        result.setFailedChecks(checks.stream()
                .filter(check -> "FAIL".equals(check.getStatus()))
                .toList());
        result.setSummary(summary(checks));
        result.setStatus(result.getFailedChecks().isEmpty() ? "PASS" : "FAIL");
        result.setNextActions(nextActions(checks, result.getStatus()));
        return sanitizeResult(result);
    }

    private Optional<StandardHealthCoverageInput> resolveCoverage(Long projectId, StandardHealthCoverageInput requestCoverage) {
        if (requestCoverage != null) {
            return Optional.of(requestCoverage);
        }
        return standardHealthSnapshotRepository.findLatestByProjectId(projectId)
                .filter(snapshot -> "collected".equalsIgnoreCase(snapshot.getCoverageStatus()))
                .map(this::coverageFromSnapshot);
    }

    private StandardHealthCoverageInput coverageFromSnapshot(StandardHealthSnapshot snapshot) {
        StandardHealthCoverageInput coverage = new StandardHealthCoverageInput();
        coverage.setCoverageRate(snapshot.getCoverageRate());
        coverage.setUnmanagedFieldCount(snapshot.getUnmanagedFieldCount());
        coverage.setMissingCommentCount(snapshot.getMissingCommentCount());
        coverage.setPossibleDuplicateCount(snapshot.getPossibleDuplicateCount());
        return coverage;
    }

    private QualityGateCheckResult coverageCheck(StandardQualityGate gate, StandardHealthCoverageInput coverage) {
        if (coverage == null || coverage.getCoverageRate() == null) {
            return warning(
                    "coverage_rate",
                    "字段覆盖率",
                    gate.getMinCoverage(),
                    "缺少最近覆盖率数据，先生成覆盖率报告或标准健康快照",
                    "/field-coverage"
            );
        }
        return minCheck(
                "coverage_rate",
                "字段覆盖率",
                coverage.getCoverageRate(),
                gate.getMinCoverage(),
                "/field-coverage",
                "处理未纳管字段并重新生成覆盖率报告"
        );
    }

    private QualityGateCheckResult unmanagedCheck(StandardQualityGate gate, StandardHealthCoverageInput coverage) {
        if (coverage == null || coverage.getUnmanagedFieldCount() == null) {
            return warning(
                    "new_unmanaged_fields",
                    "未纳管字段数",
                    gate.getMaxNewUnmanagedFields(),
                    "缺少未纳管字段统计，先生成覆盖率报告",
                    "/field-coverage"
            );
        }
        return maxCheck(
                "new_unmanaged_fields",
                "未纳管字段数",
                (double) coverage.getUnmanagedFieldCount(),
                gate.getMaxNewUnmanagedFields(),
                "/field-coverage",
                "从覆盖率报告跳转反向导入或字段库收敛未纳管字段"
        );
    }

    private QualityGateCheckResult sensitiveCheck(StandardQualityGate gate, int sensitiveGapCount) {
        if (!Boolean.TRUE.equals(gate.getRequiredSensitiveMarking())) {
            return skipped(
                    "sensitive_marking",
                    "敏感字段标记",
                    "当前策略未要求疑似敏感字段必须标注",
                    "/field-quality"
            );
        }
        return maxCheck(
                "sensitive_marking",
                "疑似敏感未标记字段数",
                (double) sensitiveGapCount,
                0,
                "/field-quality",
                "确认疑似敏感字段并设置 sensitive 标记"
        );
    }

    private QualityGateCheckResult lintCheck(StandardQualityGate gate, QualityGateLintSummary lintSummary) {
        if (lintSummary == null || lintSummary.getErrorCount() == null) {
            return warning(
                    "lint_error_issues",
                    "SQL lint ERROR 数",
                    gate.getMaxErrorIssues(),
                    "未提供 SQL lint 摘要，本次门禁未判断 SQL ERROR",
                    "/sql-lint"
            );
        }
        return maxCheck(
                "lint_error_issues",
                "SQL lint ERROR 数",
                (double) lintSummary.getErrorCount(),
                gate.getMaxErrorIssues(),
                "/sql-lint",
                "先修复 SQL lint ERROR 后再运行质量门禁"
        );
    }

    private QualityGateCheckResult minCheck(String code,
                                            String label,
                                            Double actual,
                                            Integer expected,
                                            String route,
                                            String nextAction) {
        if (expected == null) {
            return skipped(code, label, "该阈值未配置", route);
        }
        boolean passed = actual != null && actual >= expected;
        return new QualityGateCheckResult(
                code,
                label,
                passed ? "PASS" : "FAIL",
                passed ? "INFO" : "ERROR",
                actual,
                (double) expected,
                ">=",
                label + " 当前 " + format(actual) + "，阈值 >= " + expected,
                route,
                passed ? "保持当前质量水平" : nextAction
        );
    }

    private QualityGateCheckResult maxCheck(String code,
                                            String label,
                                            Double actual,
                                            Integer expected,
                                            String route,
                                            String nextAction) {
        if (expected == null) {
            return skipped(code, label, "该阈值未配置", route);
        }
        boolean passed = actual != null && actual <= expected;
        return new QualityGateCheckResult(
                code,
                label,
                passed ? "PASS" : "FAIL",
                passed ? "INFO" : "ERROR",
                actual,
                (double) expected,
                "<=",
                label + " 当前 " + format(actual) + "，阈值 <= " + expected,
                route,
                passed ? "保持当前问题数量" : nextAction
        );
    }

    private QualityGateCheckResult warning(String code, String label, Integer expected, String message, String route) {
        return new QualityGateCheckResult(
                code,
                label,
                "WARNING",
                "WARNING",
                null,
                expected == null ? null : (double) expected,
                "available",
                message,
                route,
                message
        );
    }

    private QualityGateCheckResult skipped(String code, String label, String message, String route) {
        return new QualityGateCheckResult(
                code,
                label,
                "SKIPPED",
                "INFO",
                null,
                null,
                "disabled",
                message,
                route,
                message
        );
    }

    private int sensitiveGapCount(FieldQualityReport report) {
        int count = 0;
        for (var item : report.getFields()) {
            for (FieldQualityIssue issue : item.getIssues()) {
                if ("sensitive_not_marked".equals(issue.getCode())) {
                    count++;
                }
            }
        }
        return count;
    }

    private QualityGateSummary summary(List<QualityGateCheckResult> checks) {
        QualityGateSummary summary = new QualityGateSummary();
        summary.setTotalChecks(checks.size());
        for (QualityGateCheckResult check : checks) {
            switch (check.getStatus()) {
                case "PASS" -> summary.setPassedChecks(summary.getPassedChecks() + 1);
                case "FAIL" -> summary.setFailedChecks(summary.getFailedChecks() + 1);
                case "WARNING" -> summary.setWarningChecks(summary.getWarningChecks() + 1);
                case "SKIPPED" -> summary.setSkippedChecks(summary.getSkippedChecks() + 1);
                default -> {
                }
            }
        }
        return summary;
    }

    private List<String> nextActions(List<QualityGateCheckResult> checks, String status) {
        LinkedHashSet<String> actions = new LinkedHashSet<>();
        checks.stream()
                .filter(check -> "FAIL".equals(check.getStatus()))
                .map(QualityGateCheckResult::getNextAction)
                .forEach(actions::add);
        checks.stream()
                .filter(check -> "WARNING".equals(check.getStatus()))
                .map(QualityGateCheckResult::getNextAction)
                .forEach(actions::add);
        if (actions.isEmpty()) {
            actions.add("PASS".equals(status) ? "质量门禁通过，可继续当前交付" : "查看质量门禁配置");
        }
        return List.copyOf(actions);
    }

    private StandardQualityGateResult sanitizeResult(StandardQualityGateResult result) {
        for (QualityGateCheckResult check : result.getChecks()) {
            check.setMessage(SensitiveDataSanitizer.redactText(check.getMessage()));
            check.setNextAction(SensitiveDataSanitizer.redactText(check.getNextAction()));
        }
        result.setNextActions(result.getNextActions().stream()
                .map(SensitiveDataSanitizer::redactText)
                .toList());
        return result;
    }

    private StandardQualityGate loadOrDefault(Long projectId) {
        return standardQualityGateRepository.findByProjectId(projectId)
                .orElseGet(() -> defaultGate(projectId));
    }

    private StandardQualityGate defaultGate(Long projectId) {
        StandardQualityGate gate = new StandardQualityGate();
        gate.setProjectId(projectId);
        gate.setEnabled(false);
        gate.setMinCoverage(DEFAULT_MIN_COVERAGE);
        gate.setMinAverageFieldScore(DEFAULT_MIN_AVERAGE_FIELD_SCORE);
        gate.setMaxErrorIssues(DEFAULT_MAX_ERROR_ISSUES);
        gate.setMaxNewUnmanagedFields(DEFAULT_MAX_NEW_UNMANAGED_FIELDS);
        gate.setRequiredSensitiveMarking(true);
        return gate;
    }

    private StandardQualityGateConfig toConfig(StandardQualityGate gate) {
        StandardQualityGateConfig config = new StandardQualityGateConfig();
        config.setProjectId(gate.getProjectId());
        config.setEnabled(Boolean.TRUE.equals(gate.getEnabled()));
        config.setMinCoverage(gate.getMinCoverage());
        config.setMinAverageFieldScore(gate.getMinAverageFieldScore());
        config.setMaxErrorIssues(gate.getMaxErrorIssues());
        config.setMaxNewUnmanagedFields(gate.getMaxNewUnmanagedFields());
        config.setRequiredSensitiveMarking(gate.getRequiredSensitiveMarking());
        config.setUpdatedAt(gate.getUpdatedAt());
        return config;
    }

    private void validate(StandardQualityGateSaveReq req) {
        if (req == null) {
            throw new BizException("质量门禁配置不能为空");
        }
        validatePercent(req.getMinCoverage(), "minCoverage");
        validatePercent(req.getMinAverageFieldScore(), "minAverageFieldScore");
        validateNonNegative(req.getMaxErrorIssues(), "maxErrorIssues");
        validateNonNegative(req.getMaxNewUnmanagedFields(), "maxNewUnmanagedFields");
    }

    private void validateEvaluateInput(StandardQualityGateEvaluateReq req) {
        if (req == null) {
            return;
        }
        StandardHealthCoverageInput coverage = req.getCoverage();
        if (coverage != null) {
            validatePercent(coverage.getCoverageRate(), "coverage.coverageRate");
            validateNonNegative(coverage.getUnmanagedFieldCount(), "coverage.unmanagedFieldCount");
            validateNonNegative(coverage.getMissingCommentCount(), "coverage.missingCommentCount");
            validateNonNegative(coverage.getPossibleDuplicateCount(), "coverage.possibleDuplicateCount");
        }
        QualityGateLintSummary lintSummary = req.getLintSummary();
        if (lintSummary != null) {
            validateNonNegative(lintSummary.getErrorCount(), "lintSummary.errorCount");
            validateNonNegative(lintSummary.getWarningCount(), "lintSummary.warningCount");
            validateNonNegative(lintSummary.getSuggestionCount(), "lintSummary.suggestionCount");
        }
    }

    private void validatePercent(Integer value, String field) {
        if (value != null && (value < 0 || value > 100)) {
            throw new BizException(field + " 必须在 0 到 100 之间");
        }
    }

    private void validatePercent(Double value, String field) {
        if (value != null && (value < 0 || value > 100)) {
            throw new BizException(field + " 必须在 0 到 100 之间");
        }
    }

    private void validateNonNegative(Integer value, String field) {
        if (value != null && value < 0) {
            throw new BizException(field + " 不能为负数");
        }
    }

    private void requireProject(Long projectId) {
        if (projectId == null) {
            throw new BizException("项目ID不能为空");
        }
        ProjectAccessGuard.requireProjectAccess(projectId);
    }

    private int valueOrDefault(Integer value, int defaultValue) {
        return value == null ? defaultValue : value;
    }

    private String format(Double value) {
        if (value == null) {
            return "N/A";
        }
        if (Math.floor(value) == value) {
            return String.valueOf(value.intValue());
        }
        return String.valueOf(value);
    }
}
