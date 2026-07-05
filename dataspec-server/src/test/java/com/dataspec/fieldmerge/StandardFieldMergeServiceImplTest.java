package com.dataspec.fieldmerge;

import com.dataspec.changelog.service.StandardChangeLogService;
import com.dataspec.common.exception.BizException;
import com.dataspec.field.entity.Field;
import com.dataspec.field.repository.FieldRepository;
import com.dataspec.fieldmerge.model.StandardFieldMergeApplyReq;
import com.dataspec.fieldmerge.model.StandardFieldMergePreview;
import com.dataspec.fieldmerge.model.StandardFieldMergePreviewReq;
import com.dataspec.fieldmerge.model.StandardFieldMergeResult;
import com.dataspec.fieldmerge.service.impl.StandardFieldMergeServiceImpl;
import com.dataspec.reverseimport.entity.FieldSource;
import com.dataspec.reverseimport.repository.FieldSourceRepository;
import com.dataspec.security.context.DataSpecSecurityContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 标准字段合并服务测试。
 */
class StandardFieldMergeServiceImplTest {

    @AfterEach
    void tearDown() {
        DataSpecSecurityContext.clear();
    }

    @Test
    void preview_rejectsUnsafePairsWithoutWriting() {
        FieldRepository fieldRepository = mock(FieldRepository.class);
        FieldSourceRepository sourceRepository = mock(FieldSourceRepository.class);
        StandardChangeLogService changeLogService = mock(StandardChangeLogService.class);
        StandardFieldMergeServiceImpl service = service(fieldRepository, sourceRepository, changeLogService);
        Field target = field(10L, 1L, "mobile_no", "手机号", "varchar(20)", true, "phone", "contact", "enabled");
        Field foreign = field(20L, 2L, "mobile", "手机", "varchar(20)", true, "mobile", "pii", "enabled");
        Field mergedSource = field(30L, 1L, "old_mobile", "旧手机", "varchar(20)", true, "old_mobile", "legacy", "deprecated");
        mergedSource.setReplacementFieldId(10L);

        when(fieldRepository.findById(10L)).thenReturn(Optional.of(target));
        when(fieldRepository.findById(20L)).thenReturn(Optional.of(foreign));
        when(fieldRepository.findById(30L)).thenReturn(Optional.of(mergedSource));

        assertThrows(BizException.class, () -> service.preview(new StandardFieldMergePreviewReq(1L, 10L, 10L)));
        assertThrows(BizException.class, () -> service.preview(new StandardFieldMergePreviewReq(1L, 10L, 20L)));
        assertThrows(BizException.class, () -> service.preview(new StandardFieldMergePreviewReq(1L, 10L, 30L)));

        verify(fieldRepository, never()).update(any());
        verify(changeLogService, never()).recordChange(any(), any(), any(), any(), any(), any());
    }

    @Test
    void preview_buildsSafeAliasTagMigrationAndReviewRisks() {
        FieldRepository fieldRepository = mock(FieldRepository.class);
        FieldSourceRepository sourceRepository = mock(FieldSourceRepository.class);
        StandardFieldMergeServiceImpl service = service(fieldRepository, sourceRepository, mock(StandardChangeLogService.class));
        Field target = field(10L, 1L, "mobile_no", "手机号", "varchar(20)", true, "phone", "contact", "enabled");
        Field source = field(20L, 1L, "user_mobile", "用户手机", "varchar(20)", false, "mobile,phone", "pii", "enabled");
        source.setExampleValue("13800138000");
        source.setFormatPattern("^1\\d{10}$");
        source.setFormatNotes("历史手机号字段");
        when(fieldRepository.findById(10L)).thenReturn(Optional.of(target));
        when(fieldRepository.findById(20L)).thenReturn(Optional.of(source));
        when(fieldRepository.findAllByProjectId(1L)).thenReturn(List.of(target, source));
        when(sourceRepository.findByFieldId(20L)).thenReturn(List.of(sourceRecord(20L, "customer", "user_mobile")));

        StandardFieldMergePreview preview = service.preview(new StandardFieldMergePreviewReq(1L, 10L, 20L));

        assertEquals("standard_field_merge_preview", preview.kind());
        assertEquals(1, preview.schemaVersion());
        assertEquals(10L, preview.recommendedTargetFieldId());
        assertEquals("mobile_no", preview.target().name());
        assertContainsAll(preview.targetAfter().aliases(), "phone", "mobile", "user_mobile", "用户手机");
        assertContainsAll(preview.targetAfter().tags(), "contact", "pii");
        assertTrue(preview.changes().stream().anyMatch(change ->
                "aliases".equals(change.attribute()) && "SAFE_MERGE".equals(change.migrationMode())));
        assertTrue(preview.changes().stream().anyMatch(change ->
                "exampleValue".equals(change.attribute()) && "MANUAL_REVIEW".equals(change.migrationMode())));
        assertTrue(preview.risks().stream().anyMatch(risk ->
                "NULLABILITY_MISMATCH".equals(risk.code()) && !risk.blocking()));
        assertTrue(preview.impactItems().stream().anyMatch(impact ->
                impact.description().contains("customer.user_mobile")));
        assertFalse(preview.rollbackHints().isEmpty());
        assertFalse(preview.nextActions().isEmpty());
    }

    @Test
    void preview_flagsFormatMismatchWhenOnlyTargetHasFormatNotes() {
        FieldRepository fieldRepository = mock(FieldRepository.class);
        FieldSourceRepository sourceRepository = mock(FieldSourceRepository.class);
        StandardFieldMergeServiceImpl service = service(fieldRepository, sourceRepository, mock(StandardChangeLogService.class));
        Field target = field(10L, 1L, "mobile_no", "手机号", "varchar(20)", true, "phone", "contact", "enabled");
        target.setFormatPattern("^1\\d{10}$");
        Field source = field(20L, 1L, "user_mobile", "用户手机", "varchar(20)", true, "mobile", "pii", "enabled");
        when(fieldRepository.findById(10L)).thenReturn(Optional.of(target));
        when(fieldRepository.findById(20L)).thenReturn(Optional.of(source));
        when(fieldRepository.findAllByProjectId(1L)).thenReturn(List.of(target, source));

        StandardFieldMergePreview preview = service.preview(new StandardFieldMergePreviewReq(1L, 10L, 20L));

        assertTrue(preview.risks().stream().anyMatch(risk ->
                "FORMAT_MISMATCH".equals(risk.code()) && !risk.blocking()));
    }

    @Test
    void apply_requiresReasonAndRejectsBlockingAliasRisk() {
        FieldRepository fieldRepository = mock(FieldRepository.class);
        FieldSourceRepository sourceRepository = mock(FieldSourceRepository.class);
        StandardChangeLogService changeLogService = mock(StandardChangeLogService.class);
        StandardFieldMergeServiceImpl service = service(fieldRepository, sourceRepository, changeLogService);
        Field target = field(10L, 1L, "mobile_no", "手机号", "varchar(20)", true, "phone", "contact", "enabled");
        Field source = field(20L, 1L, "user_mobile", "用户手机", "varchar(20)", true, "mobile", "pii", "enabled");
        Field aliasOwner = field(30L, 1L, "legacy_mobile", "历史手机", "varchar(20)", true, "mobile", "legacy", "enabled");
        when(fieldRepository.findById(10L)).thenReturn(Optional.of(target));
        when(fieldRepository.findById(20L)).thenReturn(Optional.of(source));
        when(fieldRepository.findAllByProjectId(1L)).thenReturn(List.of(target, source, aliasOwner));

        assertThrows(BizException.class, () -> service.apply(new StandardFieldMergeApplyReq(1L, 10L, 20L, " ")));
        assertThrows(BizException.class, () -> service.apply(new StandardFieldMergeApplyReq(1L, 10L, 20L, "统一手机号字段")));

        verify(fieldRepository, never()).update(any());
        verify(changeLogService, never()).recordChange(any(), any(), any(), any(), any(), any());
    }

    @Test
    void apply_rejectsWhenSourceWasMergedConcurrentlyWithoutUpdatingTarget() {
        FieldRepository fieldRepository = mock(FieldRepository.class);
        FieldSourceRepository sourceRepository = mock(FieldSourceRepository.class);
        StandardChangeLogService changeLogService = mock(StandardChangeLogService.class);
        StandardFieldMergeServiceImpl service = service(fieldRepository, sourceRepository, changeLogService);
        Field target = field(10L, 1L, "mobile_no", "手机号", "varchar(20)", true, "phone", "contact", "enabled");
        Field source = field(20L, 1L, "user_mobile", "用户手机", "varchar(20)", true, "mobile", "pii", "enabled");
        when(fieldRepository.findById(10L)).thenReturn(Optional.of(target));
        when(fieldRepository.findById(20L)).thenReturn(Optional.of(source));
        when(fieldRepository.findAllByProjectId(1L)).thenReturn(List.of(target, source));
        when(fieldRepository.deprecateSourceForMergeIfReplacementUnset(any())).thenReturn(0);

        assertThrows(BizException.class, () -> service.apply(new StandardFieldMergeApplyReq(
                1L, 10L, 20L, "统一手机号字段")));

        assertEquals("phone", target.getAliases());
        assertEquals("contact", target.getTags());
        verify(fieldRepository, never()).update(target);
        verify(changeLogService, never()).recordChange(any(), any(), any(), any(), any(), any());
    }

    @Test
    void apply_updatesTargetAndDeprecatesSourceWithChangeLogs() {
        FieldRepository fieldRepository = mock(FieldRepository.class);
        FieldSourceRepository sourceRepository = mock(FieldSourceRepository.class);
        StandardChangeLogService changeLogService = mock(StandardChangeLogService.class);
        StandardFieldMergeServiceImpl service = service(fieldRepository, sourceRepository, changeLogService);
        Field target = field(10L, 1L, "mobile_no", "手机号", "varchar(20)", true, "phone", "contact", "enabled");
        Field source = field(20L, 1L, "user_mobile", "用户手机", "varchar(20)", true, "mobile", "pii", "enabled");
        when(fieldRepository.findById(10L)).thenReturn(Optional.of(target));
        when(fieldRepository.findById(20L)).thenReturn(Optional.of(source));
        when(fieldRepository.findAllByProjectId(1L)).thenReturn(List.of(target, source));
        when(sourceRepository.findByFieldId(20L)).thenReturn(List.of(sourceRecord(20L, "customer", "user_mobile")));
        when(fieldRepository.deprecateSourceForMergeIfReplacementUnset(source)).thenReturn(1);
        when(fieldRepository.update(target)).thenReturn(1);
        when(changeLogService.snapshot(any(Field.class))).thenAnswer(invocation -> {
            Field field = invocation.getArgument(0);
            return field.getName() + "|" + field.getAliases() + "|" + field.getTags()
                    + "|" + field.getStatus() + "|" + field.getReplacementFieldId();
        });

        StandardFieldMergeResult result = service.apply(new StandardFieldMergeApplyReq(
                1L, 10L, 20L, "统一使用 mobile_no"));

        assertTrue(result.applied());
        assertContainsAll(splitCsv(target.getAliases()), "phone", "mobile", "user_mobile", "用户手机");
        assertContainsAll(splitCsv(target.getTags()), "contact", "pii");
        assertEquals("deprecated", source.getStatus());
        assertEquals(10L, source.getReplacementFieldId());
        assertTrue(source.getReplacementReason().contains("统一使用 mobile_no"));
        assertTrue(source.getReplacementReason().contains("merged_from=user_mobile"));
        verify(fieldRepository).update(target);
        verify(fieldRepository).deprecateSourceForMergeIfReplacementUnset(source);
        verify(changeLogService).recordChange(eq(1L), eq("field"), eq(10L), eq("update"), any(), any());
        verify(changeLogService).recordChange(eq(1L), eq("field"), eq(20L), eq("update"), any(), any());
        assertTrue(result.rollbackHints().stream().anyMatch(hint -> hint.targetPath().contains("/api/fields/10/undo")));
    }

    private StandardFieldMergeServiceImpl service(FieldRepository fieldRepository,
                                                  FieldSourceRepository sourceRepository,
                                                  StandardChangeLogService changeLogService) {
        return new StandardFieldMergeServiceImpl(fieldRepository, sourceRepository, changeLogService);
    }

    private Field field(Long id, Long projectId, String name, String displayName, String dataType,
                        Boolean nullable, String aliases, String tags, String status) {
        Field field = new Field();
        field.setId(id);
        field.setProjectId(projectId);
        field.setName(name);
        field.setDisplayName(displayName);
        field.setDataType(dataType);
        field.setNullable(nullable);
        field.setAliases(aliases);
        field.setTags(tags);
        field.setStatus(status);
        return field;
    }

    private FieldSource sourceRecord(Long fieldId, String tableName, String columnName) {
        FieldSource source = new FieldSource();
        source.setProjectId(1L);
        source.setFieldId(fieldId);
        source.setTableName(tableName);
        source.setColumnName(columnName);
        source.setSourceType("DATABASE");
        return source;
    }

    private List<String> splitCsv(String value) {
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .toList();
    }

    private void assertContainsAll(List<String> actual, String... expected) {
        for (String item : expected) {
            assertTrue(actual.contains(item), () -> "missing item: " + item + " in " + actual);
        }
    }
}
