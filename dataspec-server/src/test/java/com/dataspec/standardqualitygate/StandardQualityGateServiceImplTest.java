package com.dataspec.standardqualitygate;

import com.dataspec.common.exception.BizException;
import com.dataspec.fieldquality.model.FieldQualityIssue;
import com.dataspec.fieldquality.model.FieldQualityItem;
import com.dataspec.fieldquality.model.FieldQualityReport;
import com.dataspec.fieldquality.model.FieldQualitySeverity;
import com.dataspec.fieldquality.model.FieldQualitySummary;
import com.dataspec.fieldquality.service.FieldQualityService;
import com.dataspec.standardhealth.entity.StandardHealthSnapshot;
import com.dataspec.standardhealth.model.StandardHealthCoverageInput;
import com.dataspec.standardhealth.repository.StandardHealthSnapshotRepository;
import com.dataspec.standardqualitygate.entity.StandardQualityGate;
import com.dataspec.standardqualitygate.model.QualityGateLintSummary;
import com.dataspec.standardqualitygate.model.StandardQualityGateEvaluateReq;
import com.dataspec.standardqualitygate.model.StandardQualityGateSaveReq;
import com.dataspec.standardqualitygate.repository.StandardQualityGateRepository;
import com.dataspec.standardqualitygate.service.impl.StandardQualityGateServiceImpl;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StandardQualityGateServiceImplTest {

    @Test
    void getConfig_returnsDefaultDisabledPolicyWhenMissing() {
        StandardQualityGateRepository repository = mock(StandardQualityGateRepository.class);
        when(repository.findByProjectId(1L)).thenReturn(Optional.empty());
        StandardQualityGateServiceImpl service = service(repository, mock(FieldQualityService.class), mock(StandardHealthSnapshotRepository.class));

        var config = service.getConfig(1L);

        assertThat(config.getProjectId()).isEqualTo(1L);
        assertThat(config.getEnabled()).isFalse();
        assertThat(config.getMinCoverage()).isEqualTo(80);
        assertThat(config.getMaxErrorIssues()).isZero();
        assertThat(config.getRequiredSensitiveMarking()).isTrue();
    }

    @Test
    void saveConfig_validatesAndPersistsThresholds() {
        StandardQualityGateRepository repository = mock(StandardQualityGateRepository.class);
        when(repository.findByProjectId(1L)).thenReturn(Optional.empty());
        ArgumentCaptor<StandardQualityGate> saved = ArgumentCaptor.forClass(StandardQualityGate.class);
        when(repository.insert(saved.capture())).thenReturn(1);
        StandardQualityGateServiceImpl service = service(repository, mock(FieldQualityService.class), mock(StandardHealthSnapshotRepository.class));
        StandardQualityGateSaveReq req = saveReq(1L, true, 90, 88, 0, 2, true);

        var config = service.saveConfig(req);

        assertThat(config.getEnabled()).isTrue();
        assertThat(saved.getValue().getMinCoverage()).isEqualTo(90);
        assertThat(saved.getValue().getMinAverageFieldScore()).isEqualTo(88);
        assertThat(saved.getValue().getMaxNewUnmanagedFields()).isEqualTo(2);
    }

    @Test
    void saveConfig_rejectsInvalidThresholds() {
        StandardQualityGateServiceImpl service = service(
                mock(StandardQualityGateRepository.class),
                mock(FieldQualityService.class),
                mock(StandardHealthSnapshotRepository.class)
        );
        StandardQualityGateSaveReq req = saveReq(1L, true, 101, 80, 0, 0, true);

        assertThrows(BizException.class, () -> service.saveConfig(req));
    }

    @Test
    void evaluate_returnsFailWithOrderedActionsForBrokenSignals() {
        StandardQualityGateRepository repository = mock(StandardQualityGateRepository.class);
        when(repository.findByProjectId(1L)).thenReturn(Optional.of(gate(true, 85, 80, 0, 1, true)));
        FieldQualityService fieldQualityService = mock(FieldQualityService.class);
        when(fieldQualityService.report(1L)).thenReturn(qualityReport(70, 1, true));
        StandardHealthSnapshotRepository snapshotRepository = mock(StandardHealthSnapshotRepository.class);
        StandardQualityGateServiceImpl service = service(repository, fieldQualityService, snapshotRepository);
        StandardQualityGateEvaluateReq req = new StandardQualityGateEvaluateReq();
        req.setProjectId(1L);
        StandardHealthCoverageInput coverage = new StandardHealthCoverageInput();
        coverage.setCoverageRate(72.0);
        coverage.setUnmanagedFieldCount(3);
        req.setCoverage(coverage);
        QualityGateLintSummary lint = new QualityGateLintSummary();
        lint.setErrorCount(2);
        req.setLintSummary(lint);

        var result = service.evaluate(req);

        assertThat(result.getStatus()).isEqualTo("FAIL");
        assertThat(result.getFailedChecks()).extracting("code")
                .contains(
                        "average_field_score",
                        "field_error_issues",
                        "sensitive_marking",
                        "coverage_rate",
                        "new_unmanaged_fields",
                        "lint_error_issues"
                );
        assertThat(result.getNextActions()).contains("先修复 SQL lint ERROR 后再运行质量门禁");
        assertThat(result.toString()).doesNotContain("jdbc:", "password=", "Bearer ");
    }

    @Test
    void evaluate_usesLatestSnapshotAndWarnsForMissingLintSummary() {
        StandardQualityGateRepository repository = mock(StandardQualityGateRepository.class);
        when(repository.findByProjectId(1L)).thenReturn(Optional.of(gate(true, 80, 80, 0, 2, false)));
        FieldQualityService fieldQualityService = mock(FieldQualityService.class);
        when(fieldQualityService.report(1L)).thenReturn(qualityReport(95, 0, false));
        StandardHealthSnapshotRepository snapshotRepository = mock(StandardHealthSnapshotRepository.class);
        when(snapshotRepository.findLatestByProjectId(1L)).thenReturn(Optional.of(snapshot(90.0, 1)));
        StandardQualityGateServiceImpl service = service(repository, fieldQualityService, snapshotRepository);
        StandardQualityGateEvaluateReq req = new StandardQualityGateEvaluateReq();
        req.setProjectId(1L);

        var result = service.evaluate(req);

        assertThat(result.getStatus()).isEqualTo("PASS");
        assertThat(result.getChecks()).filteredOn(check -> "lint_error_issues".equals(check.getCode()))
                .extracting("status")
                .containsExactly("WARNING");
        assertThat(result.getChecks()).filteredOn(check -> "sensitive_marking".equals(check.getCode()))
                .extracting("status")
                .containsExactly("SKIPPED");
    }

    @Test
    void evaluate_rejectsInvalidCoverageInput() {
        StandardQualityGateRepository repository = mock(StandardQualityGateRepository.class);
        StandardQualityGateServiceImpl service = service(
                repository,
                mock(FieldQualityService.class),
                mock(StandardHealthSnapshotRepository.class)
        );
        StandardQualityGateEvaluateReq req = new StandardQualityGateEvaluateReq();
        req.setProjectId(1L);
        StandardHealthCoverageInput coverage = new StandardHealthCoverageInput();
        coverage.setCoverageRate(101.0);
        req.setCoverage(coverage);

        assertThrows(BizException.class, () -> service.evaluate(req));
    }

    @Test
    void evaluate_rejectsNegativeLintSummary() {
        StandardQualityGateRepository repository = mock(StandardQualityGateRepository.class);
        StandardQualityGateServiceImpl service = service(
                repository,
                mock(FieldQualityService.class),
                mock(StandardHealthSnapshotRepository.class)
        );
        StandardQualityGateEvaluateReq req = new StandardQualityGateEvaluateReq();
        req.setProjectId(1L);
        QualityGateLintSummary lint = new QualityGateLintSummary();
        lint.setErrorCount(-1);
        req.setLintSummary(lint);

        assertThrows(BizException.class, () -> service.evaluate(req));
    }

    private StandardQualityGateServiceImpl service(
            StandardQualityGateRepository repository,
            FieldQualityService fieldQualityService,
            StandardHealthSnapshotRepository snapshotRepository
    ) {
        return new StandardQualityGateServiceImpl(repository, fieldQualityService, snapshotRepository);
    }

    private StandardQualityGateSaveReq saveReq(Long projectId,
                                               Boolean enabled,
                                               Integer minCoverage,
                                               Integer minAverageFieldScore,
                                               Integer maxErrorIssues,
                                               Integer maxNewUnmanagedFields,
                                               Boolean requiredSensitiveMarking) {
        StandardQualityGateSaveReq req = new StandardQualityGateSaveReq();
        req.setProjectId(projectId);
        req.setEnabled(enabled);
        req.setMinCoverage(minCoverage);
        req.setMinAverageFieldScore(minAverageFieldScore);
        req.setMaxErrorIssues(maxErrorIssues);
        req.setMaxNewUnmanagedFields(maxNewUnmanagedFields);
        req.setRequiredSensitiveMarking(requiredSensitiveMarking);
        return req;
    }

    private StandardQualityGate gate(boolean enabled,
                                     int minCoverage,
                                     int minAverageFieldScore,
                                     int maxErrorIssues,
                                     int maxNewUnmanagedFields,
                                     boolean requiredSensitiveMarking) {
        StandardQualityGate gate = new StandardQualityGate();
        gate.setId(7L);
        gate.setProjectId(1L);
        gate.setEnabled(enabled);
        gate.setMinCoverage(minCoverage);
        gate.setMinAverageFieldScore(minAverageFieldScore);
        gate.setMaxErrorIssues(maxErrorIssues);
        gate.setMaxNewUnmanagedFields(maxNewUnmanagedFields);
        gate.setRequiredSensitiveMarking(requiredSensitiveMarking);
        return gate;
    }

    private FieldQualityReport qualityReport(int averageScore, int errorIssueCount, boolean sensitiveGap) {
        FieldQualityReport report = new FieldQualityReport();
        FieldQualitySummary summary = report.getSummary();
        summary.setAverageScore(averageScore);
        summary.setErrorIssueCount(errorIssueCount);
        summary.setTotalFieldCount(3);
        if (sensitiveGap) {
            FieldQualityItem item = new FieldQualityItem();
            item.setName("user_token");
            item.getIssues().add(new FieldQualityIssue(
                    "sensitive_not_marked",
                    FieldQualitySeverity.ERROR,
                    "字段疑似敏感数据但未标记 sensitive",
                    "确认是否敏感并设置敏感标记",
                    20
            ));
            report.getFields().add(item);
        }
        return report;
    }

    private StandardHealthSnapshot snapshot(Double coverageRate, Integer unmanagedCount) {
        StandardHealthSnapshot snapshot = new StandardHealthSnapshot();
        snapshot.setProjectId(1L);
        snapshot.setCoverageStatus("collected");
        snapshot.setCoverageRate(coverageRate);
        snapshot.setUnmanagedFieldCount(unmanagedCount);
        return snapshot;
    }
}
