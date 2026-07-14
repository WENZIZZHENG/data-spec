package com.dataspec.reviewfinding;

import com.dataspec.aioutputcheck.model.AiOutputPostCheckIssue;
import com.dataspec.aioutputcheck.model.AiOutputPostCheckIssueSeverity;
import com.dataspec.lint.model.FixChangeStatus;
import com.dataspec.lint.model.FixRiskLevel;
import com.dataspec.lint.model.LintIssue;
import com.dataspec.lint.model.Severity;
import com.dataspec.reviewfinding.model.ReviewFinding;
import com.dataspec.reviewfinding.model.ReviewFindingSeverity;
import com.dataspec.reviewfinding.service.ReviewFindingAdapter;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReviewFindingAdapterTest {

    @Test
    void lintIssueMapsLocationWaiverAndOnlyAppliedLowRiskFixIsSafe() {
        LintIssue issue = LintIssue.builder()
                .severity(Severity.WARNING)
                .ruleCode("column_naming")
                .ruleName("字段命名")
                .message("password=raw 应改名")
                .tableName("orders")
                .columnName("BadName")
                .suggestion("改为 bad_name")
                .confidence(95)
                .fixRiskLevel(FixRiskLevel.LOW)
                .fixStatus(FixChangeStatus.APPLIED)
                .line(2)
                .column(5)
                .lineEnd(2)
                .columnEnd(12)
                .sourceStart(30)
                .sourceEnd(37)
                .locationKind("column")
                .build();

        ReviewFinding finding = ReviewFindingAdapter.fromLintIssues(
                List.of(issue), 7L, "db/schema.sql", List.of("dataspec://evidence/sql-check/9")).getFirst();

        assertEquals(ReviewFindingSeverity.WARNING, finding.severity());
        assertEquals("db/schema.sql", finding.location().path());
        assertEquals(7L, finding.subject().projectId());
        assertTrue(finding.autoFixSafe());
        assertFalse(finding.waiver().waived());
        assertTrue(finding.observed().contains("[REDACTED]"));
        assertEquals("dataspec://evidence/sql-check/9", finding.evidenceRefs().getFirst());
    }

    @Test
    void suppressedIssueMapsWaiverAndCannotClaimAutoFixSafety() {
        LintIssue issue = LintIssue.builder()
                .severity(Severity.ERROR)
                .ruleCode("required_column")
                .ruleName("必备字段")
                .message("缺少 created_at")
                .fixRiskLevel(FixRiskLevel.LOW)
                .fixStatus(FixChangeStatus.APPLIED)
                .suppressed(true)
                .suppressionId(3L)
                .suppressionReason("兼容旧表")
                .build();

        ReviewFinding finding = ReviewFindingAdapter.fromLintIssues(List.of(issue), 1L, null, List.of()).getFirst();

        assertTrue(finding.waiver().waived());
        assertEquals(3L, finding.waiver().waiverId());
        assertFalse(finding.autoFixSafe());
    }

    @Test
    void postCheckIssueMapsAndDuplicateKeyIsStable() {
        AiOutputPostCheckIssue issue = new AiOutputPostCheckIssue(
                "UNKNOWN_STANDARD_REFERENCE",
                AiOutputPostCheckIssueSeverity.FAIL,
                null,
                "unknown_field",
                "引用无法解析",
                "select unknown_field",
                null,
                List.of(),
                List.of("改用 stableRef"));

        List<ReviewFinding> first = ReviewFindingAdapter.fromPostCheckIssues(List.of(issue, issue), 1L);
        List<ReviewFinding> second = ReviewFindingAdapter.fromPostCheckIssues(List.of(issue), 1L);

        assertEquals(1, first.size());
        assertEquals(first.getFirst().findingKey(), second.getFirst().findingKey());
        assertEquals(ReviewFindingSeverity.ERROR, first.getFirst().severity());
    }
}
