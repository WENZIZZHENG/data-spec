package com.dataspec.reviewfinding.service;

import com.dataspec.aioutputcheck.model.AiOutputPostCheckIssue;
import com.dataspec.aioutputcheck.model.AiOutputPostCheckIssueSeverity;
import com.dataspec.lint.model.FixChangeStatus;
import com.dataspec.lint.model.FixRiskLevel;
import com.dataspec.lint.model.LintIssue;
import com.dataspec.lint.model.Severity;
import com.dataspec.reviewfinding.model.ReviewFinding;
import com.dataspec.reviewfinding.model.ReviewFindingLocation;
import com.dataspec.reviewfinding.model.ReviewFindingSeverity;
import com.dataspec.reviewfinding.model.ReviewFindingSource;
import com.dataspec.reviewfinding.model.ReviewFindingSubject;
import com.dataspec.reviewfinding.model.ReviewFindingWaiver;

import java.util.LinkedHashMap;
import java.util.List;

/**
 * 将既有 lint/post-check issue 适配为 additive Review Finding，不改变旧 issue 契约。
 */
public final class ReviewFindingAdapter {

    private ReviewFindingAdapter() {
    }

    /**
     * 将 SQL lint issues 映射为共享 findings。
     *
     * @param issues 既有 lint issues
     * @param projectId 当前项目 ID，可为空
     * @param path 业务仓库相对路径；后端仅收到 SQL 文本时为空
     * @param evidenceRefs 当前 SQL check 的 canonical evidence refs
     * @return 按 findingKey 去重且保持首次出现顺序的 findings
     */
    public static List<ReviewFinding> fromLintIssues(
            List<LintIssue> issues,
            Long projectId,
            String path,
            List<String> evidenceRefs
    ) {
        if (issues == null || issues.isEmpty()) {
            return List.of();
        }
        LinkedHashMap<String, ReviewFinding> findings = new LinkedHashMap<>();
        for (LintIssue issue : issues) {
            if (issue == null) {
                continue;
            }
            boolean waived = Boolean.TRUE.equals(issue.getSuppressed());
            String subjectKind = issue.getColumnName() != null ? "SQL_COLUMN"
                    : issue.getTableName() != null ? "SQL_TABLE" : "SQL";
            String subjectName = issue.getColumnName() != null ? issue.getColumnName()
                    : issue.getTableName() != null ? issue.getTableName() : issue.getRuleName();
            ReviewFinding finding = new ReviewFinding(
                    ReviewFindingSource.SQL_LINT,
                    null,
                    issue.getRuleCode(),
                    severity(issue.getSeverity()),
                    new ReviewFindingSubject(projectId, subjectKind, subjectName,
                            issue.getTableName(), issue.getColumnName(), null),
                    new ReviewFindingLocation(path, issue.getLine(), issue.getColumn(), issue.getLineEnd(),
                            issue.getColumnEnd(), issue.getSourceStart(), issue.getSourceEnd(), issue.getLocationKind()),
                    issue.getRuleName(),
                    issue.getSuggestion(),
                    issue.getMessage(),
                    evidenceRefs,
                    issue.getConfidence(),
                    issue.getSuggestion(),
                    !waived && issue.getFixRiskLevel() == FixRiskLevel.LOW
                            && issue.getFixStatus() == FixChangeStatus.APPLIED,
                    new ReviewFindingWaiver(waived, issue.getSuppressionId(), issue.getSuppressionReason()));
            findings.putIfAbsent(finding.findingKey(), finding);
        }
        return List.copyOf(findings.values());
    }

    /** 将既有 AI output post-check issues 映射为共享 findings。 */
    public static List<ReviewFinding> fromPostCheckIssues(
            List<AiOutputPostCheckIssue> issues,
            Long projectId
    ) {
        if (issues == null || issues.isEmpty()) {
            return List.of();
        }
        LinkedHashMap<String, ReviewFinding> findings = new LinkedHashMap<>();
        for (AiOutputPostCheckIssue issue : issues) {
            if (issue == null) {
                continue;
            }
            String expected = issue.replacementRef() != null ? issue.replacementRef()
                    : issue.nextActions().stream().findFirst().orElse(null);
            ReviewFinding finding = new ReviewFinding(
                    ReviewFindingSource.AI_OUTPUT_POSTCHECK,
                    null,
                    issue.code(),
                    issue.severity() == AiOutputPostCheckIssueSeverity.FAIL
                            ? ReviewFindingSeverity.ERROR : ReviewFindingSeverity.WARNING,
                    new ReviewFindingSubject(projectId,
                            issue.refType() == null ? "AI_OUTPUT" : "STANDARD_REFERENCE",
                            issue.inputRef(), null, null, issue.inputRef()),
                    null,
                    issue.message(),
                    expected,
                    issue.excerpt(),
                    issue.evidenceLinks(),
                    null,
                    expected,
                    false,
                    ReviewFindingWaiver.NONE);
            findings.putIfAbsent(finding.findingKey(), finding);
        }
        return List.copyOf(findings.values());
    }

    /** 按稳定 findingKey 去重，保留首次出现顺序。 */
    public static List<ReviewFinding> deduplicate(List<ReviewFinding> findings) {
        if (findings == null || findings.isEmpty()) {
            return List.of();
        }
        LinkedHashMap<String, ReviewFinding> deduplicated = new LinkedHashMap<>();
        for (ReviewFinding finding : findings) {
            if (finding != null) {
                deduplicated.putIfAbsent(finding.findingKey(), finding);
            }
        }
        return List.copyOf(deduplicated.values());
    }

    private static ReviewFindingSeverity severity(Severity severity) {
        if (severity == null) {
            return ReviewFindingSeverity.WARNING;
        }
        return switch (severity) {
            case ERROR -> ReviewFindingSeverity.ERROR;
            case WARNING -> ReviewFindingSeverity.WARNING;
            case SUGGESTION -> ReviewFindingSeverity.SUGGESTION;
        };
    }
}
