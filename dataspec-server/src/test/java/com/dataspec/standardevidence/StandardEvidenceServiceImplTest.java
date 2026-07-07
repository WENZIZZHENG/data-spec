package com.dataspec.standardevidence;

import com.dataspec.aireplay.entity.AiJobRecord;
import com.dataspec.aireplay.repository.AiJobRecordRepository;
import com.dataspec.changelog.entity.StandardChangeLog;
import com.dataspec.changelog.repository.StandardChangeLogRepository;
import com.dataspec.common.exception.BizException;
import com.dataspec.field.entity.Field;
import com.dataspec.field.service.FieldService;
import com.dataspec.fieldprovenance.model.FieldProvenanceConfidenceItem;
import com.dataspec.fieldprovenance.model.FieldProvenanceConfidenceLevel;
import com.dataspec.fieldprovenance.model.FieldProvenanceConfidenceReport;
import com.dataspec.fieldprovenance.model.FieldProvenanceConfidenceSummary;
import com.dataspec.fieldprovenance.service.FieldProvenanceConfidenceService;
import com.dataspec.reverseimport.entity.FieldSource;
import com.dataspec.reverseimport.repository.FieldSourceRepository;
import com.dataspec.standardcandidate.entity.StandardCandidate;
import com.dataspec.standardcandidate.repository.StandardCandidateRepository;
import com.dataspec.standardevidence.model.StandardEvidenceItem;
import com.dataspec.standardevidence.service.impl.StandardEvidenceServiceImpl;
import com.dataspec.standardusageheatmap.model.StandardUsageHeatmapItem;
import com.dataspec.standardusageheatmap.model.StandardUsageHeatmapReport;
import com.dataspec.standardusageheatmap.model.StandardUsageHeatmapSummary;
import com.dataspec.standardusageheatmap.service.StandardUsageHeatmapService;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class StandardEvidenceServiceImplTest {

    @Test
    void report_aggregatesFieldEvidenceIntoAiSafeSummary() {
        FieldService fieldService = mock(FieldService.class);
        FieldSourceRepository sourceRepository = mock(FieldSourceRepository.class);
        FieldProvenanceConfidenceService confidenceService = mock(FieldProvenanceConfidenceService.class);
        StandardUsageHeatmapService heatmapService = mock(StandardUsageHeatmapService.class);
        StandardCandidateRepository candidateRepository = mock(StandardCandidateRepository.class);
        StandardChangeLogRepository changeLogRepository = mock(StandardChangeLogRepository.class);
        AiJobRecordRepository aiJobRepository = mock(AiJobRecordRepository.class);
        StandardEvidenceServiceImpl service = new StandardEvidenceServiceImpl(
                fieldService,
                sourceRepository,
                confidenceService,
                heatmapService,
                candidateRepository,
                changeLogRepository,
                aiJobRepository);
        LocalDateTime sourceTime = LocalDateTime.of(2026, 7, 7, 9, 0);
        LocalDateTime candidateTime = LocalDateTime.of(2026, 7, 7, 10, 0);
        LocalDateTime changeTime = LocalDateTime.of(2026, 7, 7, 11, 0);
        LocalDateTime usageTime = LocalDateTime.of(2026, 7, 7, 12, 0);
        Field field = field(10L, 1L, "mobile_no", "手机号", "enabled");
        when(fieldService.getById(10L)).thenReturn(field);
        when(sourceRepository.findSummaryByProjectId(1L)).thenReturn(List.of(
                fieldSource(100L, 10L, "database password=secret", sourceTime)));
        when(confidenceService.report(1L)).thenReturn(confidenceReport());
        when(heatmapService.report(1L)).thenReturn(heatmapReport(usageTime));
        when(candidateRepository.findSummaryByProjectId(1L)).thenReturn(List.of(candidate(candidateTime)));
        when(changeLogRepository.findSummaryByTarget(1L, "field", 10L, 20)).thenReturn(List.of(changeLog(changeTime)));
        when(aiJobRepository.findRecentSummaryByProjectId(1L, 200)).thenReturn(List.of(
                aiJob("生成 mobile_no 的 SQL", "Authorization=Bearer raw accessToken=raw", usageTime),
                aiJob("生成 old_mobile_no 的 SQL", "token=raw clientSecret=raw", usageTime.minusMinutes(20))));

        var report = service.report(1L, "FIELD", 10L);

        assertThat(report.projectId()).isEqualTo(1L);
        assertThat(report.subject().subjectType()).isEqualTo("FIELD");
        assertThat(report.subject().subjectId()).isEqualTo(10L);
        assertThat(report.subject().name()).isEqualTo("mobile_no");
        assertThat(report.summary().totalEvidenceCount()).isGreaterThanOrEqualTo(7);
        assertThat(report.summary().confidenceLevel()).isEqualTo("REVIEW");
        assertThat(report.summary().lintHitCount()).isEqualTo(2);
        assertThat(report.summary().aiJobHitCount()).isEqualTo(1);
        assertThat(report.summary().candidateDecisionCount()).isEqualTo(1);
        assertThat(report.summary().changeLogCount()).isEqualTo(1);
        assertThat(report.items()).extracting(StandardEvidenceItem::evidenceType)
                .contains(
                        "FIELD_SOURCE",
                        "PROVENANCE_CONFIDENCE",
                        "USAGE_HEATMAP",
                        "CANDIDATE_DECISION",
                        "CHANGE_LOG",
                        "SQL_LINT_HIT",
                        "AI_JOB_USAGE");
        assertThat(report.aiEvidenceSummary())
                .contains("mobile_no", "REVIEW", "主要来源 database", "候选/变更摘要", "SQL 检查命中 2 次", "AI 作业命中 1 次")
                .doesNotContain("password", "secret", "token", "Authorization", "accessToken", "clientSecret", "jdbcUrl", "jdbc:", "select mobile_no");
        assertThat(report.toString()).doesNotContain(
                "password",
                "secret",
                "token",
                "Authorization",
                "accessToken",
                "clientSecret",
                "jdbcUrl",
                "jdbc:mysql",
                "select mobile_no",
                "before raw",
                "after raw",
                "candidate raw evidence");
        verify(sourceRepository).findSummaryByProjectId(1L);
        verify(candidateRepository).findSummaryByProjectId(1L);
        verify(changeLogRepository).findSummaryByTarget(1L, "field", 10L, 20);
        verify(sourceRepository, never()).findByProjectId(1L);
        verify(candidateRepository, never()).findByProjectId(1L);
        verify(changeLogRepository, never()).findByProjectId(1L, 20);
    }

    @Test
    void report_marksLowEvidenceFieldForReview() {
        FieldService fieldService = mock(FieldService.class);
        FieldSourceRepository sourceRepository = mock(FieldSourceRepository.class);
        FieldProvenanceConfidenceService confidenceService = mock(FieldProvenanceConfidenceService.class);
        StandardUsageHeatmapService heatmapService = mock(StandardUsageHeatmapService.class);
        StandardCandidateRepository candidateRepository = mock(StandardCandidateRepository.class);
        StandardChangeLogRepository changeLogRepository = mock(StandardChangeLogRepository.class);
        AiJobRecordRepository aiJobRepository = mock(AiJobRecordRepository.class);
        StandardEvidenceServiceImpl service = new StandardEvidenceServiceImpl(
                fieldService,
                sourceRepository,
                confidenceService,
                heatmapService,
                candidateRepository,
                changeLogRepository,
                aiJobRepository);
        Field field = field(10L, 1L, "mobile_no", "手机号", "draft");
        when(fieldService.getById(10L)).thenReturn(field);
        when(sourceRepository.findSummaryByProjectId(1L)).thenReturn(List.of());
        when(confidenceService.report(1L)).thenReturn(new FieldProvenanceConfidenceReport(
                1L,
                new FieldProvenanceConfidenceSummary(1, 0, 0, 0, 1, 0, 0, 1),
                List.of(new FieldProvenanceConfidenceItem(
                        10L,
                        "mobile_no",
                        "手机号",
                        "draft",
                        "manual",
                        List.of(),
                        0,
                        0,
                        0,
                        null,
                        null,
                        40,
                        FieldProvenanceConfidenceLevel.UNKNOWN,
                        "缺少可信来源证据，需人工确认后再纳入强制标准。",
                        List.of("字段缺少来源证据")))));
        when(heatmapService.report(1L)).thenReturn(new StandardUsageHeatmapReport(
                1L,
                new StandardUsageHeatmapSummary(1, 0, 0, 0, 1, 0),
                List.of()));
        when(candidateRepository.findSummaryByProjectId(1L)).thenReturn(List.of());
        when(changeLogRepository.findSummaryByTarget(1L, "field", 10L, 20)).thenReturn(List.of());
        when(aiJobRepository.findRecentSummaryByProjectId(1L, 200)).thenReturn(List.of());

        var report = service.report(1L, "FIELD", 10L);

        assertThat(report.summary().totalEvidenceCount()).isEqualTo(1);
        assertThat(report.summary().reviewRequired()).isTrue();
        assertThat(report.aiEvidenceSummary()).contains("证据不足", "人工复核");
        assertThat(report.coverageNotes()).contains(
                "缺少字段来源证据",
                "缺少候选决策证据",
                "缺少近期 SQL 检查命中",
                "缺少近期 AI 作业命中");
    }

    @Test
    void report_rejectsUnsupportedSubjectTypeBeforeQueryingField() {
        FieldService fieldService = mock(FieldService.class);
        StandardEvidenceServiceImpl service = new StandardEvidenceServiceImpl(
                fieldService,
                mock(FieldSourceRepository.class),
                mock(FieldProvenanceConfidenceService.class),
                mock(StandardUsageHeatmapService.class),
                mock(StandardCandidateRepository.class),
                mock(StandardChangeLogRepository.class),
                mock(AiJobRecordRepository.class));

        assertThatThrownBy(() -> service.report(1L, "TABLE", 10L))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("仅支持 FIELD");

        verifyNoInteractions(fieldService);
    }

    @Test
    void report_rejectsMissingOrCrossProjectField() {
        FieldService fieldService = mock(FieldService.class);
        StandardEvidenceServiceImpl service = new StandardEvidenceServiceImpl(
                fieldService,
                mock(FieldSourceRepository.class),
                mock(FieldProvenanceConfidenceService.class),
                mock(StandardUsageHeatmapService.class),
                mock(StandardCandidateRepository.class),
                mock(StandardChangeLogRepository.class),
                mock(AiJobRecordRepository.class));
        when(fieldService.getById(10L)).thenReturn(null);
        Field otherProjectField = field(11L, 2L, "email", "邮箱", "enabled");
        when(fieldService.getById(11L)).thenReturn(otherProjectField);

        assertThatThrownBy(() -> service.report(1L, "FIELD", 10L))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("标准字段不存在或不属于当前项目");
        assertThatThrownBy(() -> service.report(1L, "FIELD", 11L))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("标准字段不存在或不属于当前项目");
    }

    private Field field(Long id, Long projectId, String name, String displayName, String status) {
        Field field = new Field();
        field.setId(id);
        field.setProjectId(projectId);
        field.setName(name);
        field.setDisplayName(displayName);
        field.setDataType("varchar(20)");
        field.setStatus(status);
        return field;
    }

    private FieldSource fieldSource(Long id, Long fieldId, String sourceType, LocalDateTime createdAt) {
        FieldSource source = new FieldSource();
        source.setId(id);
        source.setProjectId(1L);
        source.setFieldId(fieldId);
        source.setSourceType(sourceType);
        source.setCreatedAt(createdAt);
        source.setMetadataJson("{\"jdbcUrl\":\"jdbc:mysql://localhost/db?password=secret\"}");
        return source;
    }

    private FieldProvenanceConfidenceReport confidenceReport() {
        return new FieldProvenanceConfidenceReport(
                1L,
                new FieldProvenanceConfidenceSummary(1, 0, 1, 0, 0, 1, 1, 1),
                List.of(new FieldProvenanceConfidenceItem(
                        10L,
                        "mobile_no",
                        "手机号",
                        "enabled",
                        "database",
                        List.of("jdbcUrl=jdbc:mysql://localhost/db?password=secret accessToken=raw clientSecret=raw"),
                        1,
                        1,
                        2,
                        88,
                        "GOOD",
                        72,
                        FieldProvenanceConfidenceLevel.REVIEW,
                        "可用于 AI 推荐，但生成前建议复核来源证据与字段质量。token=raw",
                        List.of("存在未决标准候选 password=secret jdbcUrl=raw"))));
    }

    private StandardUsageHeatmapReport heatmapReport(LocalDateTime usageTime) {
        return new StandardUsageHeatmapReport(
                1L,
                new StandardUsageHeatmapSummary(1, 1, 1, 0, 0, 80),
                List.of(new StandardUsageHeatmapItem(
                        10L,
                        "mobile_no",
                        "手机号",
                        "enabled",
                        List.of("database"),
                        88,
                        "GOOD",
                        0,
                        1,
                        2,
                        1,
                        usageTime,
                        80,
                        75,
                        "字段近期使用较高，保持标准稳定并关注变更影响。")));
    }

    private StandardCandidate candidate(LocalDateTime decidedAt) {
        StandardCandidate candidate = new StandardCandidate();
        candidate.setId(200L);
        candidate.setProjectId(1L);
        candidate.setCandidateName("mobile_no");
        candidate.setSourceType("contract");
        candidate.setSourceRef("Authorization=Bearer raw accessToken=raw");
        candidate.setEvidenceJson("candidate raw evidence password=secret");
        candidate.setConfidence(88);
        candidate.setStatus("ACCEPTED");
        candidate.setTargetFieldId(10L);
        candidate.setDecisionReason("采纳为手机号标准 token=raw clientSecret=raw");
        candidate.setDecidedAt(decidedAt);
        return candidate;
    }

    private StandardChangeLog changeLog(LocalDateTime changedAt) {
        StandardChangeLog log = new StandardChangeLog();
        log.setId(300L);
        log.setProjectId(1L);
        log.setTargetType("field");
        log.setTargetId(10L);
        log.setAction("update");
        log.setOperatorName("Alice token=raw");
        log.setBeforeJson("before raw password=secret");
        log.setAfterJson("after raw Authorization=Bearer raw");
        log.setChangedAt(changedAt);
        return log;
    }

    private AiJobRecord aiJob(String title, String inputSummary, LocalDateTime createdAt) {
        AiJobRecord record = new AiJobRecord();
        record.setProjectId(1L);
        record.setJobType("SQL_GENERATE");
        record.setTitle(title);
        record.setInputSummary(inputSummary);
        record.setInputPayloadJson("raw payload password=secret");
        record.setOutputPayloadJson("output token=raw");
        record.setPromptVersion("v1");
        record.setStatus("SUCCEEDED");
        record.setCreatedAt(createdAt);
        return record;
    }
}
