package com.dataspec.fieldprovenance;

import com.dataspec.field.entity.Field;
import com.dataspec.field.service.FieldService;
import com.dataspec.fieldprovenance.model.FieldProvenanceConfidenceLevel;
import com.dataspec.fieldprovenance.service.impl.FieldProvenanceConfidenceServiceImpl;
import com.dataspec.fieldquality.model.FieldQualityItem;
import com.dataspec.fieldquality.model.FieldQualityLevel;
import com.dataspec.fieldquality.model.FieldQualityReport;
import com.dataspec.fieldquality.service.FieldQualityService;
import com.dataspec.reverseimport.entity.FieldSource;
import com.dataspec.reverseimport.repository.FieldSourceRepository;
import com.dataspec.standardcandidate.entity.StandardCandidate;
import com.dataspec.standardcandidate.repository.StandardCandidateRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FieldProvenanceConfidenceServiceImplTest {

    @Test
    void report_marksHighEvidenceFieldAsVerifiedAndSanitizesSourceRefs() {
        FieldService fieldService = mock(FieldService.class);
        FieldSourceRepository fieldSourceRepository = mock(FieldSourceRepository.class);
        StandardCandidateRepository candidateRepository = mock(StandardCandidateRepository.class);
        FieldQualityService fieldQualityService = mock(FieldQualityService.class);
        FieldProvenanceConfidenceServiceImpl service = new FieldProvenanceConfidenceServiceImpl(
                fieldService,
                fieldSourceRepository,
                candidateRepository,
                fieldQualityService);

        Field field = field(10L, "mobile_no", "手机号", "enabled");
        FieldSource source = source(10L, "database", "public", "users", "mobile_no");
        StandardCandidate accepted = candidate(20L, "mobile_no", "ACCEPTED", 10L, 92,
                "database-import", "jdbc:postgresql://prod/db password=secret");
        when(fieldService.listByProject(1L)).thenReturn(List.of(field));
        when(fieldSourceRepository.findByProjectId(1L)).thenReturn(List.of(source));
        when(candidateRepository.findByProjectId(1L)).thenReturn(List.of(accepted));
        when(fieldQualityService.report(1L)).thenReturn(qualityReport(qualityItem(10L, "mobile_no", 95, FieldQualityLevel.GOOD)));

        var report = service.report(1L);

        assertThat(report.summary().verifiedCount()).isEqualTo(1);
        assertThat(report.summary().fieldsWithSourceEvidence()).isEqualTo(1);
        assertThat(report.summary().fieldsWithCandidateEvidence()).isEqualTo(1);
        assertThat(report.fields()).hasSize(1);
        var item = report.fields().getFirst();
        assertThat(item.fieldId()).isEqualTo(10L);
        assertThat(item.confidenceLevel()).isEqualTo(FieldProvenanceConfidenceLevel.VERIFIED);
        assertThat(item.aiConfidence()).isGreaterThanOrEqualTo(85);
        assertThat(item.evidenceCount()).isEqualTo(2);
        assertThat(item.sourceEvidenceCount()).isEqualTo(1);
        assertThat(item.candidateEvidenceCount()).isEqualTo(1);
        assertThat(item.qualityScore()).isEqualTo(95);
        assertThat(item.sourceRefs()).anySatisfy(ref -> assertThat(ref).contains("database:public.users.mobile_no"));
        assertThat(item.sourceRefs().toString()).doesNotContain("jdbc:postgresql://prod/db", "secret");
        assertThat(item.warnings()).isEmpty();
        assertThat(item.recommendedUse()).contains("首选标准字段");
        verify(fieldService).listByProject(1L);
        verify(fieldSourceRepository).findByProjectId(1L);
        verify(candidateRepository).findByProjectId(1L);
        verify(fieldQualityService).report(1L);
    }

    @Test
    void report_flagsLowEvidenceFieldForReview() {
        FieldService fieldService = mock(FieldService.class);
        FieldSourceRepository fieldSourceRepository = mock(FieldSourceRepository.class);
        StandardCandidateRepository candidateRepository = mock(StandardCandidateRepository.class);
        FieldQualityService fieldQualityService = mock(FieldQualityService.class);
        FieldProvenanceConfidenceServiceImpl service = new FieldProvenanceConfidenceServiceImpl(
                fieldService,
                fieldSourceRepository,
                candidateRepository,
                fieldQualityService);

        Field draft = field(11L, "customer_phone", "客户电话", "draft");
        StandardCandidate pending = candidate(21L, "customer_phone", "PENDING", null, 35,
                "coverage", "文档推断");
        when(fieldService.listByProject(1L)).thenReturn(List.of(draft));
        when(fieldSourceRepository.findByProjectId(1L)).thenReturn(List.of());
        when(candidateRepository.findByProjectId(1L)).thenReturn(List.of(pending));
        when(fieldQualityService.report(1L)).thenReturn(qualityReport(qualityItem(11L, "customer_phone", 45, FieldQualityLevel.POOR)));

        var item = service.report(1L).fields().getFirst();

        assertThat(item.confidenceLevel()).isEqualTo(FieldProvenanceConfidenceLevel.LOW);
        assertThat(item.aiConfidence()).isLessThan(65);
        assertThat(item.evidenceCount()).isEqualTo(1);
        assertThat(item.warnings()).contains(
                "字段缺少来源证据",
                "存在未决标准候选，使用前需要处理 Inbox",
                "字段状态为 draft，不应作为强制标准直接使用",
                "字段质量评分偏低");
        assertThat(item.recommendedUse()).contains("仅作为候选参考");
    }

    @Test
    void report_marksNoEvidenceFieldAsUnknownEvenWhenQualityIsGood() {
        FieldService fieldService = mock(FieldService.class);
        FieldSourceRepository fieldSourceRepository = mock(FieldSourceRepository.class);
        StandardCandidateRepository candidateRepository = mock(StandardCandidateRepository.class);
        FieldQualityService fieldQualityService = mock(FieldQualityService.class);
        FieldProvenanceConfidenceServiceImpl service = new FieldProvenanceConfidenceServiceImpl(
                fieldService,
                fieldSourceRepository,
                candidateRepository,
                fieldQualityService);

        Field field = field(12L, "order_no", "订单号", "enabled");
        when(fieldService.listByProject(1L)).thenReturn(List.of(field));
        when(fieldSourceRepository.findByProjectId(1L)).thenReturn(List.of());
        when(candidateRepository.findByProjectId(1L)).thenReturn(List.of());
        when(fieldQualityService.report(1L)).thenReturn(qualityReport(qualityItem(12L, "order_no", 95, FieldQualityLevel.GOOD)));

        var item = service.report(1L).fields().getFirst();

        assertThat(item.confidenceLevel()).isEqualTo(FieldProvenanceConfidenceLevel.UNKNOWN);
        assertThat(item.recommendedUse()).contains("缺少可信来源证据");
        assertThat(item.warnings()).contains("字段缺少来源证据");
    }

    @Test
    void report_sanitizesPrimarySourceType() {
        FieldService fieldService = mock(FieldService.class);
        FieldSourceRepository fieldSourceRepository = mock(FieldSourceRepository.class);
        StandardCandidateRepository candidateRepository = mock(StandardCandidateRepository.class);
        FieldQualityService fieldQualityService = mock(FieldQualityService.class);
        FieldProvenanceConfidenceServiceImpl service = new FieldProvenanceConfidenceServiceImpl(
                fieldService,
                fieldSourceRepository,
                candidateRepository,
                fieldQualityService);

        Field field = field(13L, "api_token", "接口令牌", "enabled");
        FieldSource source = source(13L, "Authorization=Bearer raw-token", null, null, null);
        when(fieldService.listByProject(1L)).thenReturn(List.of(field));
        when(fieldSourceRepository.findByProjectId(1L)).thenReturn(List.of(source));
        when(candidateRepository.findByProjectId(1L)).thenReturn(List.of());
        when(fieldQualityService.report(1L)).thenReturn(qualityReport(qualityItem(13L, "api_token", 80, FieldQualityLevel.WARNING)));

        var item = service.report(1L).fields().getFirst();

        assertThat(item.primarySourceType()).contains("[REDACTED]");
        assertThat(item.primarySourceType()).doesNotContain("raw-token");
        assertThat(item.sourceRefs().toString()).doesNotContain("raw-token");
    }

    private Field field(Long id, String name, String displayName, String status) {
        Field field = new Field();
        field.setId(id);
        field.setProjectId(1L);
        field.setName(name);
        field.setDisplayName(displayName);
        field.setDataType("varchar(20)");
        field.setStatus(status);
        return field;
    }

    private FieldSource source(Long fieldId, String sourceType, String schemaName, String tableName, String columnName) {
        FieldSource source = new FieldSource();
        source.setProjectId(1L);
        source.setFieldId(fieldId);
        source.setSourceType(sourceType);
        source.setSchemaName(schemaName);
        source.setTableName(tableName);
        source.setColumnName(columnName);
        return source;
    }

    private StandardCandidate candidate(
            Long id,
            String candidateName,
            String status,
            Long targetFieldId,
            Integer confidence,
            String sourceType,
            String sourceRef
    ) {
        StandardCandidate candidate = new StandardCandidate();
        candidate.setId(id);
        candidate.setProjectId(1L);
        candidate.setCandidateName(candidateName);
        candidate.setStatus(status);
        candidate.setTargetFieldId(targetFieldId);
        candidate.setConfidence(confidence);
        candidate.setSourceType(sourceType);
        candidate.setSourceRef(sourceRef);
        return candidate;
    }

    private FieldQualityReport qualityReport(FieldQualityItem... items) {
        FieldQualityReport report = new FieldQualityReport();
        report.getFields().addAll(List.of(items));
        return report;
    }

    private FieldQualityItem qualityItem(Long fieldId, String name, int score, FieldQualityLevel level) {
        FieldQualityItem item = new FieldQualityItem();
        item.setFieldId(fieldId);
        item.setName(name);
        item.setScore(score);
        item.setLevel(level);
        return item;
    }
}
