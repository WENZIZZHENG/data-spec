package com.dataspec.standardhealth;

import com.dataspec.aifeedback.model.AiFeedbackAction;
import com.dataspec.aifeedback.model.AiFeedbackReport;
import com.dataspec.aifeedback.model.AiFeedbackSampleSize;
import com.dataspec.aifeedback.model.AiFeedbackSummary;
import com.dataspec.aifeedback.service.AiFeedbackService;
import com.dataspec.fieldquality.model.FieldQualityReport;
import com.dataspec.fieldquality.model.FieldQualitySummary;
import com.dataspec.fieldquality.service.FieldQualityService;
import com.dataspec.standardcandidate.repository.StandardCandidateRepository;
import com.dataspec.standardhealth.entity.StandardHealthSnapshot;
import com.dataspec.standardhealth.model.StandardHealthCoverageInput;
import com.dataspec.standardhealth.model.StandardHealthSnapshotCreateReq;
import com.dataspec.standardhealth.model.StandardHealthSnapshotView;
import com.dataspec.standardhealth.model.StandardHealthTrend;
import com.dataspec.standardhealth.repository.StandardHealthSnapshotRepository;
import com.dataspec.standardhealth.service.impl.StandardHealthServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StandardHealthServiceImplTest {

    @Test
    void createSnapshot_aggregatesSignalsAndStoresSanitizedPayload() {
        FieldQualityService fieldQualityService = mock(FieldQualityService.class);
        AiFeedbackService aiFeedbackService = mock(AiFeedbackService.class);
        StandardCandidateRepository candidateRepository = mock(StandardCandidateRepository.class);
        StandardHealthSnapshotRepository snapshotRepository = mock(StandardHealthSnapshotRepository.class);

        when(fieldQualityService.report(1L)).thenReturn(qualityReport(76, 3, 12));
        when(aiFeedbackService.buildReport(1L)).thenReturn(feedbackReport());
        when(candidateRepository.countByStatuses(1L, List.of("PENDING", "POSTPONED"))).thenReturn(4);
        when(candidateRepository.countByStatuses(1L, List.of("ACCEPTED", "MERGED"))).thenReturn(6);
        ArgumentCaptor<StandardHealthSnapshot> saved = ArgumentCaptor.forClass(StandardHealthSnapshot.class);
        when(snapshotRepository.insert(saved.capture())).thenAnswer(invocation -> {
            saved.getValue().setId(42L);
            return 1;
        });

        StandardHealthServiceImpl service = new StandardHealthServiceImpl(
                fieldQualityService,
                aiFeedbackService,
                candidateRepository,
                snapshotRepository,
                new ObjectMapper().findAndRegisterModules()
        );
        StandardHealthSnapshotCreateReq req = new StandardHealthSnapshotCreateReq();
        req.setProjectId(1L);
        StandardHealthCoverageInput coverage = new StandardHealthCoverageInput();
        coverage.setCoverageRate(62.5);
        coverage.setUnmanagedFieldCount(5);
        coverage.setMissingCommentCount(2);
        coverage.setPossibleDuplicateCount(1);
        coverage.setTopUnmanagedFields(List.of("legacy_user_id", "mobile_no"));
        req.setCoverage(coverage);

        StandardHealthSnapshotView view = service.createSnapshot(req);

        assertThat(view.getId()).isEqualTo(42L);
        assertThat(view.getMetrics().getAverageQualityScore()).isEqualTo(76);
        assertThat(view.getMetrics().getLowQualityFieldCount()).isEqualTo(3);
        assertThat(view.getMetrics().getCoverageStatus()).isEqualTo("collected");
        assertThat(view.getMetrics().getCoverageRate()).isEqualTo(62.5);
        assertThat(view.getMetrics().getPendingCandidateCount()).isEqualTo(4);
        assertThat(view.getMetrics().getAdoptedCandidateCount()).isEqualTo(6);
        assertThat(view.getTopActions()).extracting("priority").contains("HIGH");
        assertThat(view.getPlanMarkdown()).contains("标准健康改进计划", "补字段注释和别名");
        assertThat(saved.getValue().getPayloadJson())
                .doesNotContain("CREATE TABLE", "jdbc:", "ds_secret", "password=");
    }

    @Test
    void trend_returnsWeekAndMonthDeltasFromExistingSnapshots() {
        StandardHealthSnapshotRepository snapshotRepository = mock(StandardHealthSnapshotRepository.class);
        when(snapshotRepository.findRecentByProjectId(1L, 60)).thenReturn(List.of(
                snapshot(3L, 1L, nowMinusDays(0), 84, 1, 91.0, 2),
                snapshot(2L, 1L, nowMinusDays(8), 74, 4, 81.0, 5),
                snapshot(1L, 1L, nowMinusDays(35), 70, 6, 70.0, 8)
        ));

        StandardHealthServiceImpl service = new StandardHealthServiceImpl(
                mock(FieldQualityService.class),
                mock(AiFeedbackService.class),
                mock(StandardCandidateRepository.class),
                snapshotRepository,
                new ObjectMapper().findAndRegisterModules()
        );

        StandardHealthTrend trend = service.trend(1L, 60);

        assertThat(trend.getLatest().getId()).isEqualTo(3L);
        assertThat(trend.getSnapshots()).hasSize(3);
        assertThat(trend.getWeekDelta().getQualityAverageScoreDelta()).isEqualTo(10);
        assertThat(trend.getWeekDelta().getLowQualityFieldCountDelta()).isEqualTo(-3);
        assertThat(trend.getWeekDelta().getCoverageRateDelta()).isEqualTo(10.0);
        assertThat(trend.getWeekDelta().getUnmanagedFieldCountDelta()).isEqualTo(-3);
        assertThat(trend.getMonthDelta().getQualityAverageScoreDelta()).isEqualTo(14);
        assertThat(trend.getMonthDelta().getUnmanagedFieldCountDelta()).isEqualTo(-6);
    }

    @Test
    void trend_returnsRecoverableEmptyStateWhenNoSnapshotsExist() {
        StandardHealthSnapshotRepository snapshotRepository = mock(StandardHealthSnapshotRepository.class);
        when(snapshotRepository.findRecentByProjectId(1L, 30)).thenReturn(List.of());
        StandardHealthServiceImpl service = new StandardHealthServiceImpl(
                mock(FieldQualityService.class),
                mock(AiFeedbackService.class),
                mock(StandardCandidateRepository.class),
                snapshotRepository,
                new ObjectMapper().findAndRegisterModules()
        );

        StandardHealthTrend trend = service.trend(1L, 30);

        assertThat(trend.getSnapshots()).isEmpty();
        assertThat(trend.getLatest()).isNull();
        assertThat(trend.getNextActions()).contains("创建第一条标准健康快照");
    }

    private FieldQualityReport qualityReport(int averageScore, int lowQualityCount, int totalFieldCount) {
        FieldQualityReport report = new FieldQualityReport();
        FieldQualitySummary summary = report.getSummary();
        summary.setAverageScore(averageScore);
        summary.setLowQualityCount(lowQualityCount);
        summary.setTotalFieldCount(totalFieldCount);
        return report;
    }

    private AiFeedbackReport feedbackReport() {
        return new AiFeedbackReport(
                1L,
                new AiFeedbackSummary(2, 3, 1, 0, 2, 3, 1, true, "字段推荐历史不足"),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(new AiFeedbackAction("补字段注释和别名", "优先维护高频字段", "HIGH", "/fields")),
                new AiFeedbackSampleSize(2, 3, 1, 0, 12),
                LocalDateTime.now()
        );
    }

    private StandardHealthSnapshot snapshot(
            Long id,
            Long projectId,
            LocalDateTime capturedAt,
            int qualityScore,
            int lowQualityCount,
            Double coverageRate,
            int unmanagedCount
    ) {
        StandardHealthSnapshot snapshot = new StandardHealthSnapshot();
        snapshot.setId(id);
        snapshot.setProjectId(projectId);
        snapshot.setCapturedAt(capturedAt);
        snapshot.setAverageQualityScore(qualityScore);
        snapshot.setLowQualityFieldCount(lowQualityCount);
        snapshot.setCoverageRate(coverageRate);
        snapshot.setUnmanagedFieldCount(unmanagedCount);
        snapshot.setCoverageStatus("collected");
        snapshot.setTopActionsJson("[]");
        snapshot.setPayloadJson("{}");
        return snapshot;
    }

    private LocalDateTime nowMinusDays(int days) {
        return LocalDateTime.now().minusDays(days);
    }
}
