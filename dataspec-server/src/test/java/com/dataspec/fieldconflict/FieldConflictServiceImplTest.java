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

    @Test
    void report_detectsSqlNamingRisks() {
        FieldService fieldService = mock(FieldService.class);
        when(fieldService.listByProject(1L)).thenReturn(List.of(
                field(1L, "order", "订单", "varchar(32)", null, false, null),
                field(2L, "UserID", "用户编号", "bigint", null, false, null),
                field(3L, "userid", "用户编号小写", "bigint", null, false, null),
                field(4L, "customer_type", "客户类型", "varchar(20)", "type", false, null),
                field(5L, "type", "类型", "varchar(20)", null, false, null)
        ));
        FieldConflictServiceImpl service = new FieldConflictServiceImpl(fieldService);

        var report = service.report(1L);

        var reserved = report.getGroups().stream()
                .filter(group -> FieldConflictType.RESERVED_WORD.equals(group.getConflictType()))
                .filter(group -> group.getGroupKey().contains("order"))
                .findFirst()
                .orElseThrow();
        assertEquals(FieldConflictSeverity.WARNING, reserved.getSeverity());
        assertTrue(reserved.getEvidence().stream().anyMatch(item -> item.contains("PostgreSQL")));
        assertTrue(reserved.getSuggestedAction().contains("order_value"));

        var caseCollision = report.getGroups().stream()
                .filter(group -> FieldConflictType.CASE_COLLISION.equals(group.getConflictType()))
                .findFirst()
                .orElseThrow();
        assertTrue(caseCollision.getEvidence().stream().anyMatch(item -> item.contains("大小写")));
        assertEquals(2, caseCollision.getFields().size());

        var ambiguousAlias = report.getGroups().stream()
                .filter(group -> FieldConflictType.AMBIGUOUS_ALIAS.equals(group.getConflictType()))
                .findFirst()
                .orElseThrow();
        assertTrue(ambiguousAlias.getEvidence().stream().anyMatch(item -> item.contains("type")));
        assertTrue(ambiguousAlias.getSuggestedAction().contains("不要直接使用该 alias"));
    }

    @Test
    void report_ignoresCaseVariantAliasesOnSameField() {
        FieldService fieldService = mock(FieldService.class);
        when(fieldService.listByProject(1L)).thenReturn(List.of(
                field(1L, "user_id", "用户ID", "bigint", "UserID,userid", false, null)
        ));
        FieldConflictServiceImpl service = new FieldConflictServiceImpl(fieldService);

        var report = service.report(1L);

        assertTrue(report.getGroups().stream()
                .noneMatch(group -> FieldConflictType.CASE_COLLISION.equals(group.getConflictType())));
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
