package com.dataspec.fieldconflict;

import com.dataspec.field.entity.Field;
import com.dataspec.field.service.FieldService;
import com.dataspec.fieldconflict.model.FieldConflictSeverity;
import com.dataspec.fieldconflict.model.FieldConflictType;
import com.dataspec.fieldconflict.service.impl.FieldConflictServiceImpl;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FieldConflictServiceImplTest {

    @Test
    void report_detectsAliasConflictAndSemanticDuplicate() {
        FieldService fieldService = mock(FieldService.class);
        when(fieldService.listByProject(1L)).thenReturn(List.of(
                field(1L, "user_id", "用户ID", "bigint", "uid", false, null),
                field(2L, "account_id", "账号ID", "varchar(32)", "uid", false, null),
                field(3L, "mobile_no", "手机号", "varchar(20)", "phone,tel", true, null),
                field(4L, "phone_number", "联系电话", "varchar(20)", "mobile", false, null)
        ));
        FieldConflictServiceImpl service = new FieldConflictServiceImpl(fieldService);

        var report = service.report(1L);

        assertEquals(1L, report.getProjectId());
        assertEquals(4, report.getSummary().getTotalFieldCount());
        assertTrue(report.getSummary().getConflictGroupCount() >= 2);
        assertEquals(4, report.getSummary().getAffectedFieldCount());
        assertTrue(report.getSummary().getAliasConflictCount() >= 1);
        assertTrue(report.getSummary().getSemanticDuplicateCount() >= 1);
        assertTrue(report.getSummary().getAttributeMismatchCount() >= 1);

        var aliasGroup = report.getGroups().stream()
                .filter(group -> FieldConflictType.ALIAS_CONFLICT.equals(group.getConflictType()))
                .findFirst()
                .orElseThrow();
        assertEquals(FieldConflictSeverity.ERROR, aliasGroup.getSeverity());
        assertTrue(aliasGroup.getEvidence().stream().anyMatch(item -> item.contains("uid")));
        assertEquals(2, aliasGroup.getFields().size());

        var semanticGroup = report.getGroups().stream()
                .filter(group -> FieldConflictType.SEMANTIC_DUPLICATE.equals(group.getConflictType()))
                .filter(group -> group.getFields().stream().anyMatch(field -> "mobile_no".equals(field.getName())))
                .findFirst()
                .orElseThrow();
        assertTrue(semanticGroup.getEvidence().stream().anyMatch(item -> item.contains("敏感标记不一致")));
        assertEquals(FieldConflictSeverity.WARNING, semanticGroup.getSeverity());
    }

    @Test
    void report_returnsEmptyGroupsWhenNoConflict() {
        FieldService fieldService = mock(FieldService.class);
        when(fieldService.listByProject(1L)).thenReturn(List.of(
                field(1L, "created_at", "创建时间", "timestamp", "create_time", false, null),
                field(2L, "updated_at", "更新时间", "timestamp", "update_time", false, null)
        ));
        FieldConflictServiceImpl service = new FieldConflictServiceImpl(fieldService);

        var report = service.report(1L);

        assertEquals(2, report.getSummary().getTotalFieldCount());
        assertEquals(0, report.getSummary().getConflictGroupCount());
        assertTrue(report.getGroups().isEmpty());
    }

    private Field field(Long id, String name, String displayName, String dataType,
                        String aliases, boolean sensitive, Long codeSetId) {
        Field field = new Field();
        field.setId(id);
        field.setProjectId(1L);
        field.setName(name);
        field.setDisplayName(displayName);
        field.setDataType(dataType);
        field.setAliases(aliases);
        field.setSensitive(sensitive);
        field.setCodeSetId(codeSetId);
        field.setStatus("enabled");
        return field;
    }
}
