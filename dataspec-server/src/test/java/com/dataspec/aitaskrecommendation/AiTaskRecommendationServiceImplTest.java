package com.dataspec.aitaskrecommendation;

import com.dataspec.aitaskrecommendation.service.impl.AiTaskRecommendationServiceImpl;
import com.dataspec.standardcandidate.repository.StandardCandidateRepository;
import com.dataspec.standardhealth.model.StandardHealthAction;
import com.dataspec.standardhealth.model.StandardHealthSnapshotView;
import com.dataspec.standardhealth.model.StandardHealthTrend;
import com.dataspec.standardhealth.service.StandardHealthService;
import com.dataspec.standardqualitygate.model.QualityGateCheckResult;
import com.dataspec.standardqualitygate.model.StandardQualityGateEvaluateReq;
import com.dataspec.standardqualitygate.model.StandardQualityGateResult;
import com.dataspec.standardqualitygate.service.StandardQualityGateService;
import com.dataspec.standardusageheatmap.model.StandardUsageHeatmapItem;
import com.dataspec.standardusageheatmap.model.StandardUsageHeatmapReport;
import com.dataspec.standardusageheatmap.model.StandardUsageHeatmapSummary;
import com.dataspec.standardusageheatmap.service.StandardUsageHeatmapService;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiTaskRecommendationServiceImplTest {

    @Test
    void report_generatesPrioritizedTaskCardsFromDiagnosticsWithoutRawPayload() {
        StandardHealthService healthService = mock(StandardHealthService.class);
        StandardUsageHeatmapService heatmapService = mock(StandardUsageHeatmapService.class);
        StandardCandidateRepository candidateRepository = mock(StandardCandidateRepository.class);
        StandardQualityGateService qualityGateService = mock(StandardQualityGateService.class);
        AiTaskRecommendationServiceImpl service = new AiTaskRecommendationServiceImpl(
                healthService,
                heatmapService,
                candidateRepository,
                qualityGateService);

        when(healthService.trend(1L, 1)).thenReturn(healthTrend());
        when(heatmapService.report(1L)).thenReturn(heatmapReport());
        when(candidateRepository.countByStatuses(1L, List.of("PENDING", "POSTPONED"))).thenReturn(3);
        when(qualityGateService.evaluate(any(StandardQualityGateEvaluateReq.class))).thenReturn(failedGate());

        var report = service.report(1L);

        assertThat(report.projectId()).isEqualTo(1L);
        assertThat(report.summary().totalTaskCount()).isEqualTo(4);
        assertThat(report.summary().highPriorityCount()).isEqualTo(2);
        assertThat(report.items()).extracting("taskType")
                .contains("FIX_QUALITY_GATE", "REPAIR_HOT_STANDARD", "REVIEW_STANDARD_CANDIDATES", "CREATE_HEALTH_SNAPSHOT");
        assertThat(report.items().getFirst().priority()).isEqualTo("HIGH");
        assertThat(report.items()).allSatisfy(item -> {
            assertThat(item.title()).isNotBlank();
            assertThat(item.reason()).isNotBlank();
            assertThat(item.targetRoute()).startsWith("/");
            assertThat(item.recommendedCommand()).isNotBlank();
            assertThat(item.evidenceRefs()).isNotEmpty();
            assertThat(item.completionCheck()).isNotBlank();
        });
        assertThat(report.toString()).doesNotContain("secret", "token=raw", "Authorization");
        verify(candidateRepository).countByStatuses(1L, List.of("PENDING", "POSTPONED"));
        verify(qualityGateService).evaluate(any(StandardQualityGateEvaluateReq.class));
    }

    @Test
    void report_limitsItemsAndKeepsCandidateTaskMediumWhenOnlyFewPendingCandidates() {
        StandardHealthService healthService = mock(StandardHealthService.class);
        StandardUsageHeatmapService heatmapService = mock(StandardUsageHeatmapService.class);
        StandardCandidateRepository candidateRepository = mock(StandardCandidateRepository.class);
        StandardQualityGateService qualityGateService = mock(StandardQualityGateService.class);
        AiTaskRecommendationServiceImpl service = new AiTaskRecommendationServiceImpl(
                healthService,
                heatmapService,
                candidateRepository,
                qualityGateService);

        when(healthService.trend(1L, 1)).thenReturn(new StandardHealthTrend());
        when(heatmapService.report(1L)).thenReturn(new StandardUsageHeatmapReport(
                1L,
                new StandardUsageHeatmapSummary(0, 0, 0, 0, 0, 0),
                List.of()));
        when(candidateRepository.countByStatuses(1L, List.of("PENDING", "POSTPONED"))).thenReturn(2);
        when(qualityGateService.evaluate(any(StandardQualityGateEvaluateReq.class))).thenReturn(new StandardQualityGateResult());

        var report = service.report(1L);

        assertThat(report.items()).hasSize(3);
        assertThat(report.items()).extracting("taskType")
                .contains("REVIEW_STANDARD_CANDIDATES", "RUN_QUALITY_REPORT", "EXPORT_AI_CONTEXT");
        assertThat(report.summary().mediumPriorityCount()).isEqualTo(1);
    }

    @Test
    void report_addsFallbackTasksForLowSignalProject() {
        StandardHealthService healthService = mock(StandardHealthService.class);
        StandardUsageHeatmapService heatmapService = mock(StandardUsageHeatmapService.class);
        StandardCandidateRepository candidateRepository = mock(StandardCandidateRepository.class);
        StandardQualityGateService qualityGateService = mock(StandardQualityGateService.class);
        AiTaskRecommendationServiceImpl service = new AiTaskRecommendationServiceImpl(
                healthService,
                heatmapService,
                candidateRepository,
                qualityGateService);

        when(healthService.trend(1L, 1)).thenReturn(new StandardHealthTrend());
        when(heatmapService.report(1L)).thenReturn(new StandardUsageHeatmapReport(
                1L,
                new StandardUsageHeatmapSummary(0, 0, 0, 0, 0, 0),
                List.of()));
        when(candidateRepository.countByStatuses(1L, List.of("PENDING", "POSTPONED"))).thenReturn(0);
        when(qualityGateService.evaluate(any(StandardQualityGateEvaluateReq.class))).thenReturn(new StandardQualityGateResult());

        var report = service.report(1L);

        assertThat(report.items()).hasSize(3);
        assertThat(report.items()).extracting("taskType")
                .containsExactlyInAnyOrder("RUN_QUALITY_REPORT", "EXPORT_AI_CONTEXT", "CREATE_HEALTH_SNAPSHOT");
    }

    @Test
    void report_keepsExistingQueryStringWhenBuildingHealthActionCommandAndAvoidsDuplicateCandidateTask() {
        StandardHealthService healthService = mock(StandardHealthService.class);
        StandardUsageHeatmapService heatmapService = mock(StandardUsageHeatmapService.class);
        StandardCandidateRepository candidateRepository = mock(StandardCandidateRepository.class);
        StandardQualityGateService qualityGateService = mock(StandardQualityGateService.class);
        AiTaskRecommendationServiceImpl service = new AiTaskRecommendationServiceImpl(
                healthService,
                heatmapService,
                candidateRepository,
                qualityGateService);

        StandardHealthSnapshotView latest = new StandardHealthSnapshotView();
        latest.setTopActions(List.of(new StandardHealthAction(
                "处理标准候选 Inbox",
                "已有候选需要处理",
                "HIGH",
                "/standard-candidates?status=PENDING",
                "candidate.pending=12"
        )));
        StandardHealthTrend trend = new StandardHealthTrend();
        trend.setLatest(latest);
        when(healthService.trend(1L, 1)).thenReturn(trend);
        when(heatmapService.report(1L)).thenReturn(new StandardUsageHeatmapReport(
                1L,
                new StandardUsageHeatmapSummary(0, 0, 0, 0, 0, 0),
                List.of()));
        when(candidateRepository.countByStatuses(1L, List.of("PENDING", "POSTPONED"))).thenReturn(12);
        when(qualityGateService.evaluate(any(StandardQualityGateEvaluateReq.class))).thenReturn(new StandardQualityGateResult());

        var report = service.report(1L);

        assertThat(report.items()).filteredOn(item -> item.taskType().equals("REVIEW_STANDARD_CANDIDATES"))
                .hasSize(1)
                .first()
                .satisfies(item -> assertThat(item.recommendedCommand())
                        .contains("/standard-candidates?projectId=1&status=PENDING")
                        .doesNotContain("?status=PENDING?projectId"));
    }

    @Test
    void report_redactsSensitiveLabelsAcrossReasonRouteCommandAndEvidence() {
        StandardHealthService healthService = mock(StandardHealthService.class);
        StandardUsageHeatmapService heatmapService = mock(StandardUsageHeatmapService.class);
        StandardCandidateRepository candidateRepository = mock(StandardCandidateRepository.class);
        StandardQualityGateService qualityGateService = mock(StandardQualityGateService.class);
        AiTaskRecommendationServiceImpl service = new AiTaskRecommendationServiceImpl(
                healthService,
                heatmapService,
                candidateRepository,
                qualityGateService);

        StandardHealthSnapshotView latest = new StandardHealthSnapshotView();
        latest.setTopActions(List.of(new StandardHealthAction(
                "检查敏感配置",
                "jdbc:mysql://localhost/db?password=secret api_key=raw DSN=mysql://root:pwd@localhost/db",
                "MEDIUM",
                "/diagnostics?token=raw",
                "Authorization=Bearer raw"
        )));
        StandardHealthTrend trend = new StandardHealthTrend();
        trend.setLatest(latest);
        when(healthService.trend(1L, 1)).thenReturn(trend);
        when(heatmapService.report(1L)).thenReturn(new StandardUsageHeatmapReport(
                1L,
                new StandardUsageHeatmapSummary(0, 0, 0, 0, 0, 0),
                List.of()));
        when(candidateRepository.countByStatuses(1L, List.of("PENDING", "POSTPONED"))).thenReturn(0);
        when(qualityGateService.evaluate(any(StandardQualityGateEvaluateReq.class))).thenReturn(new StandardQualityGateResult());

        var report = service.report(1L);

        assertThat(report.toString()).doesNotContain(
                "jdbc:mysql",
                "mysql://",
                "password",
                "secret",
                "api_key",
                "token=raw",
                "Authorization",
                "DSN");
    }

    private StandardHealthTrend healthTrend() {
        StandardHealthSnapshotView latest = new StandardHealthSnapshotView();
        latest.setTopActions(List.of(new StandardHealthAction(
                "创建标准健康快照",
                "Authorization Bearer raw should not leak",
                "MEDIUM",
                "/standard-health",
                "health.snapshot=missing"
        )));
        StandardHealthTrend trend = new StandardHealthTrend();
        trend.setProjectId(1L);
        trend.setLatest(latest);
        return trend;
    }

    private StandardUsageHeatmapReport heatmapReport() {
        StandardUsageHeatmapItem item = new StandardUsageHeatmapItem(
                10L,
                "mobile_no",
                "手机号",
                "enabled",
                List.of("database"),
                92,
                "GOOD",
                1,
                1,
                1,
                1,
                LocalDateTime.of(2026, 7, 7, 9, 0),
                80,
                90,
                "高使用且存在冲突，优先修复。"
        );
        return new StandardUsageHeatmapReport(
                1L,
                new StandardUsageHeatmapSummary(1, 1, 1, 0, 0, 90),
                List.of(item));
    }

    private StandardQualityGateResult failedGate() {
        StandardQualityGateResult result = new StandardQualityGateResult();
        result.setProjectId(1L);
        result.setStatus("FAILED");
        result.getFailedChecks().add(new QualityGateCheckResult(
                "average_quality",
                "平均质量分",
                "FAILED",
                "ERROR",
                60.0,
                80.0,
                ">=",
                "质量不足 password='secret'",
                "/field-quality",
                "补字段说明 token=raw"
        ));
        return result;
    }
}
