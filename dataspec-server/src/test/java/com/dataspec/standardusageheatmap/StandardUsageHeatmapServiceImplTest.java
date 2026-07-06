package com.dataspec.standardusageheatmap;

import com.dataspec.aireplay.entity.AiJobRecord;
import com.dataspec.aireplay.repository.AiJobRecordRepository;
import com.dataspec.field.entity.Field;
import com.dataspec.field.service.FieldService;
import com.dataspec.fieldconflict.model.FieldConflictField;
import com.dataspec.fieldconflict.model.FieldConflictGroup;
import com.dataspec.fieldconflict.model.FieldConflictReport;
import com.dataspec.fieldconflict.model.FieldConflictSeverity;
import com.dataspec.fieldconflict.model.FieldConflictType;
import com.dataspec.fieldconflict.service.FieldConflictService;
import com.dataspec.fieldquality.model.FieldQualityItem;
import com.dataspec.fieldquality.model.FieldQualityLevel;
import com.dataspec.fieldquality.model.FieldQualityReport;
import com.dataspec.fieldquality.service.FieldQualityService;
import com.dataspec.lint.entity.SqlCheckRecord;
import com.dataspec.lint.repository.SqlCheckRecordRepository;
import com.dataspec.reverseimport.entity.FieldSource;
import com.dataspec.reverseimport.repository.FieldSourceRepository;
import com.dataspec.standardusageheatmap.service.impl.StandardUsageHeatmapServiceImpl;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StandardUsageHeatmapServiceImplTest {

    @Test
    void report_prioritizesHighUsageLowQualityFieldForRepairWithoutRawPayload() {
        FieldService fieldService = mock(FieldService.class);
        FieldQualityService qualityService = mock(FieldQualityService.class);
        FieldConflictService conflictService = mock(FieldConflictService.class);
        FieldSourceRepository sourceRepository = mock(FieldSourceRepository.class);
        SqlCheckRecordRepository sqlCheckRepository = mock(SqlCheckRecordRepository.class);
        AiJobRecordRepository aiJobRepository = mock(AiJobRecordRepository.class);
        StandardUsageHeatmapServiceImpl service = new StandardUsageHeatmapServiceImpl(
                fieldService,
                qualityService,
                conflictService,
                sourceRepository,
                sqlCheckRepository,
                aiJobRepository);

        Field mobile = field(10L, "mobile_no", "手机号", "enabled");
        Field old = field(11L, "old_mobile_no", "旧手机号", "deprecated");
        LocalDateTime sqlTime = LocalDateTime.of(2026, 7, 7, 9, 0);
        LocalDateTime aiTime = LocalDateTime.of(2026, 7, 7, 10, 0);
        SqlCheckRecord sql = sqlRecord("select mobile_no from users where password='secret'", sqlTime);
        AiJobRecord aiJob = aiJob("生成 mobile_no 字段 SQL", "token=raw should not leak", aiTime);
        when(fieldService.listByProject(1L)).thenReturn(List.of(mobile, old));
        when(qualityService.report(1L)).thenReturn(qualityReport(
                qualityItem(10L, "mobile_no", 45, FieldQualityLevel.POOR),
                qualityItem(11L, "old_mobile_no", 90, FieldQualityLevel.GOOD)));
        when(conflictService.report(1L, List.of(mobile, old))).thenReturn(conflictReport(conflictGroup(10L, "mobile_no")));
        when(sourceRepository.findSummaryByProjectId(1L)).thenReturn(List.of(source(10L, "database")));
        when(sqlCheckRepository.findRecentByProjectId(1L, 200)).thenReturn(List.of(sql));
        when(aiJobRepository.findRecentSummaryByProjectId(1L, 200)).thenReturn(List.of(aiJob));

        var report = service.report(1L);

        assertThat(report.summary().totalFieldCount()).isEqualTo(2);
        assertThat(report.summary().hotFieldCount()).isEqualTo(1);
        assertThat(report.items()).hasSize(2);
        var item = report.items().getFirst();
        assertThat(item.fieldId()).isEqualTo(10L);
        assertThat(item.lintHits()).isEqualTo(1);
        assertThat(item.aiJobHits()).isEqualTo(1);
        assertThat(item.sourceKinds()).containsExactly("database");
        assertThat(item.qualityScore()).isEqualTo(45);
        assertThat(item.conflictCount()).isEqualTo(1);
        assertThat(item.usageScore()).isGreaterThanOrEqualTo(70);
        assertThat(item.cleanupPriority()).isGreaterThanOrEqualTo(70);
        assertThat(item.lastReferencedAt()).isEqualTo(aiTime);
        assertThat(item.suggestedNextAction()).contains("优先修复");
        assertThat(item.toString()).doesNotContain("secret", "token=raw", "select mobile_no");
        verify(fieldService).listByProject(1L);
        verify(conflictService).report(1L, List.of(mobile, old));
        verify(sourceRepository).findSummaryByProjectId(1L);
        verify(aiJobRepository).findRecentSummaryByProjectId(1L, 200);
        verify(sourceRepository, never()).findByProjectId(1L);
        verify(aiJobRepository, never()).findRecentByProjectId(1L, 200);
    }

    @Test
    void report_suggestsCleanupForDeprecatedUnusedField() {
        FieldService fieldService = mock(FieldService.class);
        FieldQualityService qualityService = mock(FieldQualityService.class);
        FieldConflictService conflictService = mock(FieldConflictService.class);
        FieldSourceRepository sourceRepository = mock(FieldSourceRepository.class);
        SqlCheckRecordRepository sqlCheckRepository = mock(SqlCheckRecordRepository.class);
        AiJobRecordRepository aiJobRepository = mock(AiJobRecordRepository.class);
        StandardUsageHeatmapServiceImpl service = new StandardUsageHeatmapServiceImpl(
                fieldService,
                qualityService,
                conflictService,
                sourceRepository,
                sqlCheckRepository,
                aiJobRepository);

        Field old = field(11L, "old_mobile_no", "旧手机号", "deprecated");
        when(fieldService.listByProject(1L)).thenReturn(List.of(old));
        when(qualityService.report(1L)).thenReturn(qualityReport(qualityItem(11L, "old_mobile_no", 90, FieldQualityLevel.GOOD)));
        when(conflictService.report(1L, List.of(old))).thenReturn(new FieldConflictReport());
        when(sourceRepository.findSummaryByProjectId(1L)).thenReturn(List.of());
        when(sqlCheckRepository.findRecentByProjectId(1L, 200)).thenReturn(List.of());
        when(aiJobRepository.findRecentSummaryByProjectId(1L, 200)).thenReturn(List.of());

        var item = service.report(1L).items().getFirst();

        assertThat(item.usageScore()).isZero();
        assertThat(item.cleanupPriority()).isGreaterThanOrEqualTo(60);
        assertThat(item.suggestedNextAction()).contains("归档");
    }

    @Test
    void report_prioritizesHighUsageLowQualityFieldWithoutConflict() {
        FieldService fieldService = mock(FieldService.class);
        FieldQualityService qualityService = mock(FieldQualityService.class);
        FieldConflictService conflictService = mock(FieldConflictService.class);
        FieldSourceRepository sourceRepository = mock(FieldSourceRepository.class);
        SqlCheckRecordRepository sqlCheckRepository = mock(SqlCheckRecordRepository.class);
        AiJobRecordRepository aiJobRepository = mock(AiJobRecordRepository.class);
        StandardUsageHeatmapServiceImpl service = new StandardUsageHeatmapServiceImpl(
                fieldService,
                qualityService,
                conflictService,
                sourceRepository,
                sqlCheckRepository,
                aiJobRepository);

        Field field = field(10L, "mobile_no", "手机号", "enabled");
        when(fieldService.listByProject(1L)).thenReturn(List.of(field));
        when(qualityService.report(1L)).thenReturn(qualityReport(qualityItem(10L, "mobile_no", 45, FieldQualityLevel.POOR)));
        when(conflictService.report(1L, List.of(field))).thenReturn(new FieldConflictReport());
        when(sourceRepository.findSummaryByProjectId(1L)).thenReturn(List.of(source(10L, "database")));
        when(sqlCheckRepository.findRecentByProjectId(1L, 200)).thenReturn(List.of(sqlRecord("select t.mobile_no from users t", LocalDateTime.now())));
        when(aiJobRepository.findRecentSummaryByProjectId(1L, 200)).thenReturn(List.of(aiJob("生成 \"mobile_no\"", null, LocalDateTime.now())));

        var item = service.report(1L).items().getFirst();

        assertThat(item.usageScore()).isGreaterThanOrEqualTo(70);
        assertThat(item.cleanupPriority()).isGreaterThanOrEqualTo(70);
        assertThat(item.suggestedNextAction()).contains("优先修复");
    }

    @Test
    void report_prioritizesHighUsageConflictedFieldEvenWhenQualityIsGood() {
        FieldService fieldService = mock(FieldService.class);
        FieldQualityService qualityService = mock(FieldQualityService.class);
        FieldConflictService conflictService = mock(FieldConflictService.class);
        FieldSourceRepository sourceRepository = mock(FieldSourceRepository.class);
        SqlCheckRecordRepository sqlCheckRepository = mock(SqlCheckRecordRepository.class);
        AiJobRecordRepository aiJobRepository = mock(AiJobRecordRepository.class);
        StandardUsageHeatmapServiceImpl service = new StandardUsageHeatmapServiceImpl(
                fieldService,
                qualityService,
                conflictService,
                sourceRepository,
                sqlCheckRepository,
                aiJobRepository);

        Field field = field(10L, "mobile_no", "手机号", "enabled");
        when(fieldService.listByProject(1L)).thenReturn(List.of(field));
        when(qualityService.report(1L)).thenReturn(qualityReport(qualityItem(10L, "mobile_no", 92, FieldQualityLevel.GOOD)));
        when(conflictService.report(1L, List.of(field))).thenReturn(conflictReport(conflictGroup(10L, "mobile_no")));
        when(sourceRepository.findSummaryByProjectId(1L)).thenReturn(List.of(source(10L, "database")));
        when(sqlCheckRepository.findRecentByProjectId(1L, 200)).thenReturn(List.of(sqlRecord("select t.mobile_no from users t", LocalDateTime.now())));
        when(aiJobRepository.findRecentSummaryByProjectId(1L, 200)).thenReturn(List.of(aiJob("生成 mobile_no", null, LocalDateTime.now())));

        var item = service.report(1L).items().getFirst();

        assertThat(item.usageScore()).isGreaterThanOrEqualTo(70);
        assertThat(item.cleanupPriority()).isGreaterThanOrEqualTo(70);
        assertThat(item.suggestedNextAction()).contains("优先修复");
    }

    @Test
    void report_prioritizesDeprecatedUnusedFieldEvenWithSourceEvidence() {
        FieldService fieldService = mock(FieldService.class);
        FieldQualityService qualityService = mock(FieldQualityService.class);
        FieldConflictService conflictService = mock(FieldConflictService.class);
        FieldSourceRepository sourceRepository = mock(FieldSourceRepository.class);
        SqlCheckRecordRepository sqlCheckRepository = mock(SqlCheckRecordRepository.class);
        AiJobRecordRepository aiJobRepository = mock(AiJobRecordRepository.class);
        StandardUsageHeatmapServiceImpl service = new StandardUsageHeatmapServiceImpl(
                fieldService,
                qualityService,
                conflictService,
                sourceRepository,
                sqlCheckRepository,
                aiJobRepository);

        Field old = field(11L, "old_mobile_no", "旧手机号", "deprecated");
        when(fieldService.listByProject(1L)).thenReturn(List.of(old));
        when(qualityService.report(1L)).thenReturn(qualityReport(qualityItem(11L, "old_mobile_no", 90, FieldQualityLevel.GOOD)));
        when(conflictService.report(1L, List.of(old))).thenReturn(new FieldConflictReport());
        when(sourceRepository.findSummaryByProjectId(1L)).thenReturn(List.of(source(11L, "database")));
        when(sqlCheckRepository.findRecentByProjectId(1L, 200)).thenReturn(List.of());
        when(aiJobRepository.findRecentSummaryByProjectId(1L, 200)).thenReturn(List.of());

        var report = service.report(1L);
        var item = report.items().getFirst();

        assertThat(item.usageScore()).isEqualTo(10);
        assertThat(item.cleanupPriority()).isGreaterThanOrEqualTo(60);
        assertThat(report.summary().cleanupCandidateCount()).isEqualTo(1);
        assertThat(item.suggestedNextAction()).contains("归档");
    }

    @Test
    void report_doesNotSuggestArchiveForDeprecatedFieldWithRecentReference() {
        FieldService fieldService = mock(FieldService.class);
        FieldQualityService qualityService = mock(FieldQualityService.class);
        FieldConflictService conflictService = mock(FieldConflictService.class);
        FieldSourceRepository sourceRepository = mock(FieldSourceRepository.class);
        SqlCheckRecordRepository sqlCheckRepository = mock(SqlCheckRecordRepository.class);
        AiJobRecordRepository aiJobRepository = mock(AiJobRecordRepository.class);
        StandardUsageHeatmapServiceImpl service = new StandardUsageHeatmapServiceImpl(
                fieldService,
                qualityService,
                conflictService,
                sourceRepository,
                sqlCheckRepository,
                aiJobRepository);

        Field old = field(11L, "old_mobile_no", "旧手机号", "deprecated");
        when(fieldService.listByProject(1L)).thenReturn(List.of(old));
        when(qualityService.report(1L)).thenReturn(qualityReport(qualityItem(11L, "old_mobile_no", 90, FieldQualityLevel.GOOD)));
        when(conflictService.report(1L, List.of(old))).thenReturn(new FieldConflictReport());
        when(sourceRepository.findSummaryByProjectId(1L)).thenReturn(List.of());
        when(sqlCheckRepository.findRecentByProjectId(1L, 200)).thenReturn(List.of(sqlRecord("select old_mobile_no from users", LocalDateTime.now())));
        when(aiJobRepository.findRecentSummaryByProjectId(1L, 200)).thenReturn(List.of());

        var report = service.report(1L);
        var item = report.items().getFirst();

        assertThat(item.usageScore()).isGreaterThan(0).isLessThan(70);
        assertThat(report.summary().cleanupCandidateCount()).isZero();
        assertThat(item.suggestedNextAction()).doesNotContain("归档", "近期未命中");
    }

    @Test
    void report_matchesFieldNameByIdentifierBoundary() {
        FieldService fieldService = mock(FieldService.class);
        FieldQualityService qualityService = mock(FieldQualityService.class);
        FieldConflictService conflictService = mock(FieldConflictService.class);
        FieldSourceRepository sourceRepository = mock(FieldSourceRepository.class);
        SqlCheckRecordRepository sqlCheckRepository = mock(SqlCheckRecordRepository.class);
        AiJobRecordRepository aiJobRepository = mock(AiJobRecordRepository.class);
        StandardUsageHeatmapServiceImpl service = new StandardUsageHeatmapServiceImpl(
                fieldService,
                qualityService,
                conflictService,
                sourceRepository,
                sqlCheckRepository,
                aiJobRepository);

        Field field = field(10L, "mobile_no", "手机号", "enabled");
        when(fieldService.listByProject(1L)).thenReturn(List.of(field));
        when(qualityService.report(1L)).thenReturn(qualityReport(qualityItem(10L, "mobile_no", 90, FieldQualityLevel.GOOD)));
        when(conflictService.report(1L, List.of(field))).thenReturn(new FieldConflictReport());
        when(sourceRepository.findSummaryByProjectId(1L)).thenReturn(List.of());
        when(sqlCheckRepository.findRecentByProjectId(1L, 200)).thenReturn(List.of(
                sqlRecord("select old_mobile_no from users", LocalDateTime.of(2026, 7, 7, 8, 0)),
                sqlRecord("select t.mobile_no from users t", LocalDateTime.of(2026, 7, 7, 9, 0)),
                sqlRecord("select * from users", "{\"field\":\"mobile_no\"}", LocalDateTime.of(2026, 7, 7, 10, 0)),
                sqlRecord("select mymobile_no_copy from users", LocalDateTime.of(2026, 7, 7, 11, 0))));
        when(aiJobRepository.findRecentSummaryByProjectId(1L, 200)).thenReturn(List.of(aiJob("引用 \"mobile_no\"", null, LocalDateTime.of(2026, 7, 7, 12, 0))));

        var item = service.report(1L).items().getFirst();

        assertThat(item.lintHits()).isEqualTo(2);
        assertThat(item.aiJobHits()).isEqualTo(1);
        assertThat(item.lastReferencedAt()).isEqualTo(LocalDateTime.of(2026, 7, 7, 12, 0));
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

    private FieldSource source(Long fieldId, String sourceType) {
        FieldSource source = new FieldSource();
        source.setProjectId(1L);
        source.setFieldId(fieldId);
        source.setSourceType(sourceType);
        return source;
    }

    private SqlCheckRecord sqlRecord(String sql, LocalDateTime createdAt) {
        return sqlRecord(sql, null, createdAt);
    }

    private SqlCheckRecord sqlRecord(String sql, String issuesJson, LocalDateTime createdAt) {
        SqlCheckRecord record = new SqlCheckRecord();
        record.setProjectId(1L);
        record.setOriginalSql(sql);
        record.setIssuesJson(issuesJson);
        record.setCreatedAt(createdAt);
        return record;
    }

    private AiJobRecord aiJob(String title, String inputSummary, LocalDateTime createdAt) {
        AiJobRecord record = new AiJobRecord();
        record.setProjectId(1L);
        record.setTitle(title);
        record.setInputSummary(inputSummary);
        record.setCreatedAt(createdAt);
        return record;
    }

    private FieldConflictReport conflictReport(FieldConflictGroup... groups) {
        FieldConflictReport report = new FieldConflictReport();
        report.getGroups().addAll(List.of(groups));
        return report;
    }

    private FieldConflictGroup conflictGroup(Long fieldId, String fieldName) {
        FieldConflictField field = new FieldConflictField();
        field.setFieldId(fieldId);
        field.setName(fieldName);
        FieldConflictGroup group = new FieldConflictGroup();
        group.setConflictType(FieldConflictType.NAME_DUPLICATE);
        group.setSeverity(FieldConflictSeverity.ERROR);
        group.getFields().add(field);
        return group;
    }
}
