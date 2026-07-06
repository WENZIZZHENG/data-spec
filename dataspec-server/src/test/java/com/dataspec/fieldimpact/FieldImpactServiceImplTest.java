package com.dataspec.fieldimpact;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dataspec.common.exception.BizException;
import com.dataspec.field.entity.Field;
import com.dataspec.field.service.FieldService;
import com.dataspec.fieldimpact.model.FieldImpactItem;
import com.dataspec.fieldimpact.model.FieldImpactSeverity;
import com.dataspec.fieldimpact.model.FieldImpactSummary;
import com.dataspec.fieldimpact.model.FieldImpactType;
import com.dataspec.fieldimpact.service.impl.FieldImpactServiceImpl;
import com.dataspec.lint.entity.SqlCheckRecord;
import com.dataspec.lint.service.SqlCheckRecordService;
import com.dataspec.reverseimport.entity.FieldSource;
import com.dataspec.reverseimport.entity.ReverseImportBatch;
import com.dataspec.reverseimport.model.FieldSourceDetail;
import com.dataspec.reverseimport.service.ReverseImportSourceService;
import com.dataspec.standard.dto.StandardSnapshotInfo;
import com.dataspec.standard.service.StandardSnapshotService;
import com.dataspec.template.entity.Template;
import com.dataspec.template.entity.TemplateField;
import com.dataspec.template.repository.TemplateRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FieldImpactServiceImplTest {

    @Test
    void report_returnsEmptyImpactForStandaloneField() {
        TestDeps deps = newDeps();
        Field field = field(10L, 1L, "user_id", "用户ID");
        when(deps.fieldService.getById(10L)).thenReturn(field);
        when(deps.templateRepository.findByProjectId(1L)).thenReturn(List.of());
        when(deps.reverseImportSourceService.listByFieldId(10L)).thenReturn(List.of());
        when(deps.sqlCheckRecordService.listByProject(1L, 1, 20)).thenReturn(new Page<>(1, 20));
        when(deps.standardSnapshotService.listSnapshots(1L)).thenReturn(List.of());

        var report = deps.service.report(1L, 10L);

        assertThat(report.getSummary().getTotalImpactCount()).isZero();
        assertThat(report.getImpacts()).isEmpty();
        assertThat(report.getEditWarnings()).isEmpty();
        verify(deps.fieldService).getById(10L);
    }

    @Test
    void report_rejectsFieldOutsideProject() {
        TestDeps deps = newDeps();
        when(deps.fieldService.getById(10L)).thenReturn(field(10L, 2L, "user_id", "用户ID"));

        assertThatThrownBy(() -> deps.service.report(1L, 10L))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("字段不属于当前项目");
    }

    @Test
    void report_aggregatesTemplateSourceSqlAndSnapshotImpacts() {
        TestDeps deps = newDeps();
        Field field = field(10L, 1L, "user_id", "用户ID");
        field.setCodeSetId(99L);
        when(deps.fieldService.getById(10L)).thenReturn(field);

        Template template = template(20L, 1L, "订单模板");
        TemplateField templateField = new TemplateField();
        templateField.setId(30L);
        templateField.setTemplateId(20L);
        templateField.setFieldId(10L);
        templateField.setName("user_id");
        when(deps.templateRepository.findByProjectId(1L)).thenReturn(List.of(template));
        when(deps.templateRepository.findFieldsByTemplateId(20L)).thenReturn(List.of(templateField));

        when(deps.reverseImportSourceService.listByFieldId(10L)).thenReturn(List.of(sourceDetail()));

        Page<SqlCheckRecord> page = new Page<>(1, 20);
        SqlCheckRecord record = new SqlCheckRecord();
        record.setId(40L);
        record.setOriginalSql("create table orders (user_id bigint);");
        record.setCreatedAt(LocalDateTime.parse("2026-06-27T10:00:00"));
        page.setRecords(List.of(record));
        when(deps.sqlCheckRecordService.listByProject(1L, 1, 20)).thenReturn(page);

        StandardSnapshotInfo snapshot = new StandardSnapshotInfo(
                50L,
                1L,
                "v1",
                "第一版",
                "demo",
                "abc123",
                LocalDateTime.parse("2026-06-27T10:30:00"),
                true
        );
        when(deps.standardSnapshotService.listSnapshots(1L)).thenReturn(List.of(snapshot));

        var report = deps.service.report(1L, 10L);

        assertThat(report.getSummary().getTotalImpactCount()).isEqualTo(5);
        assertThat(report.getImpacts()).extracting("impactType")
                .contains(
                        FieldImpactType.TEMPLATE,
                        FieldImpactType.IMPORT_SOURCE,
                        FieldImpactType.SQL_CHECK,
                        FieldImpactType.STANDARD_SNAPSHOT,
                        FieldImpactType.CODE_SET
                );
        assertThat(report.getImpacts())
                .filteredOn(item -> item.getImpactType() == FieldImpactType.SQL_CHECK)
                .first()
                .extracting("severity")
                .isEqualTo(FieldImpactSeverity.INFO);
        assertThat(report.getEditWarnings()).extracting("attribute")
                .contains("name", "dataType", "status", "codeSetId", "sensitive");
        verify(deps.sqlCheckRecordService).listByProject(1L, 1, 20);
    }

    @Test
    void impactModelsSupportBusinessCodeReferenceSummary() {
        FieldImpactSummary summary = new FieldImpactSummary();
        summary.setCodeReferenceImpactCount(2);
        FieldImpactItem item = new FieldImpactItem();
        item.setImpactType(FieldImpactType.CODE_REFERENCE);
        item.getMetadata().put("renameRisk", "HIGH");
        item.getMetadata().put("files", List.of("sql/orders.sql"));

        assertThat(FieldImpactType.valueOf("CODE_REFERENCE")).isEqualTo(FieldImpactType.CODE_REFERENCE);
        assertThat(summary.getCodeReferenceImpactCount()).isEqualTo(2);
        assertThat(item.getImpactType()).isEqualTo(FieldImpactType.CODE_REFERENCE);
        assertThat(item.getMetadata()).containsEntry("renameRisk", "HIGH");
    }

    private static TestDeps newDeps() {
        FieldService fieldService = mock(FieldService.class);
        TemplateRepository templateRepository = mock(TemplateRepository.class);
        ReverseImportSourceService reverseImportSourceService = mock(ReverseImportSourceService.class);
        SqlCheckRecordService sqlCheckRecordService = mock(SqlCheckRecordService.class);
        StandardSnapshotService standardSnapshotService = mock(StandardSnapshotService.class);
        return new TestDeps(
                fieldService,
                templateRepository,
                reverseImportSourceService,
                sqlCheckRecordService,
                standardSnapshotService,
                new FieldImpactServiceImpl(
                        fieldService,
                        templateRepository,
                        reverseImportSourceService,
                        sqlCheckRecordService,
                        standardSnapshotService
                )
        );
    }

    private static Field field(Long id, Long projectId, String name, String displayName) {
        Field field = new Field();
        field.setId(id);
        field.setProjectId(projectId);
        field.setName(name);
        field.setDisplayName(displayName);
        field.setDataType("bigint");
        field.setStatus("enabled");
        field.setSensitive(false);
        return field;
    }

    private static Template template(Long id, Long projectId, String name) {
        Template template = new Template();
        template.setId(id);
        template.setProjectId(projectId);
        template.setName(name);
        return template;
    }

    private static FieldSourceDetail sourceDetail() {
        FieldSource source = new FieldSource();
        source.setId(60L);
        source.setProjectId(1L);
        source.setFieldId(10L);
        source.setSchemaName("public");
        source.setTableName("orders");
        source.setColumnName("user_id");
        source.setCreatedAt(LocalDateTime.parse("2026-06-27T09:00:00"));

        ReverseImportBatch batch = new ReverseImportBatch();
        batch.setId(70L);
        batch.setProjectId(1L);
        batch.setDatabaseType("postgresql");
        batch.setDatabaseName("app");
        batch.setSchemaName("public");
        batch.setCreatedAt(LocalDateTime.parse("2026-06-27T09:00:00"));
        return new FieldSourceDetail(source, batch);
    }

    private record TestDeps(
            FieldService fieldService,
            TemplateRepository templateRepository,
            ReverseImportSourceService reverseImportSourceService,
            SqlCheckRecordService sqlCheckRecordService,
            StandardSnapshotService standardSnapshotService,
            FieldImpactServiceImpl service
    ) {
    }
}
