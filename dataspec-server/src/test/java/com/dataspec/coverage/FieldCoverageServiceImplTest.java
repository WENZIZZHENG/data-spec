package com.dataspec.coverage;

import com.dataspec.common.exception.BizException;
import com.dataspec.coverage.model.FieldCoverageReport;
import com.dataspec.coverage.model.FieldCoverageStatus;
import com.dataspec.coverage.service.impl.FieldCoverageServiceImpl;
import com.dataspec.field.entity.Field;
import com.dataspec.field.model.FieldSuggestion;
import com.dataspec.field.service.FieldService;
import com.dataspec.lint.engine.SqlParserService;
import com.dataspec.lint.model.ColumnDef;
import com.dataspec.lint.model.TableDef;
import com.dataspec.reverseimport.model.DatabaseMetadataScanFailureSummary;
import com.dataspec.reverseimport.model.DatabaseMetadataScanPartialResult;
import com.dataspec.reverseimport.model.DatabaseSchemaColumn;
import com.dataspec.reverseimport.model.DatabaseSchemaTable;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class FieldCoverageServiceImplTest {

    @Test
    void reportTables_classifiesCoverageAndBuildsSummary() {
        FieldService fieldService = mock(FieldService.class);
        FieldCoverageServiceImpl service = new FieldCoverageServiceImpl(fieldService, mock(SqlParserService.class));
        Field userId = field("user_id", "用户ID", "bigint", "");
        Field mobileNo = field("mobile_no", "手机号", "varchar(20)", "phone");
        Field status = field("status", "状态", "int", "");
        when(fieldService.listByProject(1L)).thenReturn(List.of(userId, mobileNo, status));
        when(fieldService.suggest(eq(1L), eq("mobile"), eq(1))).thenReturn(List.of(new FieldSuggestion(
                mobileNo,
                88,
                "语义词命中: mobile_no",
                "mobile_no",
                true)));
        when(fieldService.suggest(eq(1L), eq("custom_flag"), eq(1))).thenReturn(List.of(new FieldSuggestion(
                null,
                0,
                "未命中已有标准字段",
                "custom_flag",
                false)));

        FieldCoverageReport report = service.reportTables(1L, List.of(TableDef.builder()
                .name("user_order")
                .comment("订单表")
                .columns(List.of(
                        column("user_id", "bigint", "用户ID"),
                        column("phone", "varchar(20)", "联系电话"),
                        column("status", "int", ""),
                        column("mobile", "varchar(20)", "手机"),
                        column("custom_flag", "boolean", "自定义标记")
                ))
                .build()));

        assertEquals(1, report.getSummary().getTableCount());
        assertEquals(5, report.getSummary().getColumnCount());
        assertEquals(3, report.getSummary().getCoveredCount());
        assertEquals(2, report.getSummary().getUnmanagedCount());
        assertEquals(1, report.getSummary().getMissingCommentCount());
        assertEquals(1, report.getSummary().getPossibleDuplicateCount());
        assertEquals(60.0, report.getSummary().getCoverageRate());

        var fields = report.getTables().get(0).getFields();
        assertEquals(FieldCoverageStatus.STANDARD_MATCH, fields.get(0).getStatus());
        assertEquals(FieldCoverageStatus.ALIAS_MATCH, fields.get(1).getStatus());
        assertEquals(FieldCoverageStatus.MISSING_COMMENT, fields.get(2).getStatus());
        assertEquals(FieldCoverageStatus.POSSIBLE_DUPLICATE, fields.get(3).getStatus());
        assertEquals("mobile_no", fields.get(3).getRecommendedFieldName());
        assertEquals(FieldCoverageStatus.UNMANAGED, fields.get(4).getStatus());
        assertEquals("custom_flag", report.getUnmanagedRankings().get(0).getColumnName());
    }

    @Test
    void reportTables_rejectsEmptyInput() {
        FieldCoverageServiceImpl service = new FieldCoverageServiceImpl(mock(FieldService.class), mock(SqlParserService.class));

        BizException ex = assertThrows(BizException.class, () -> service.reportTables(1L, List.of()));

        assertTrue(ex.getMessage().contains("未读取到可分析的表结构"));
    }

    @Test
    void reportScanPartial_marksPartialBoundaryAndIgnoresFailedOrSkippedTables() {
        FieldService fieldService = mock(FieldService.class);
        when(fieldService.listByProject(1L)).thenReturn(List.of(field("user_id", "用户ID", "bigint", "")));
        FieldCoverageServiceImpl service = new FieldCoverageServiceImpl(fieldService, mock(SqlParserService.class));
        DatabaseMetadataScanPartialResult partial = new DatabaseMetadataScanPartialResult();
        partial.getSuccessfulTables().add(schemaTable("user_order", "user_id"));
        partial.getSuccessfulTableNames().add("user_order");
        partial.getFailedTableNames().add("payment_bill");
        partial.getSkippedTableNames().add("audit_log");
        partial.setCompleteForCoverage(true);
        partial.setComplete(false);
        DatabaseMetadataScanFailureSummary failureSummary = new DatabaseMetadataScanFailureSummary();
        failureSummary.setFailedTableCount(1);
        failureSummary.getSafeNextActions().add("降低 pageSize 后使用 resumeCursor 继续扫描失败表。");
        failureSummary.getSafeNextActions().add("不要复制 password=secret jdbc:postgresql://localhost/demo Authorization: Bearer abc");

        FieldCoverageReport report = service.reportScanPartial(1L, partial, failureSummary, "PARTIAL");

        assertEquals("PARTIAL", report.getInputStatus());
        assertEquals(1, report.getFailedTableCount());
        assertEquals(1, report.getSkippedTableCount());
        assertTrue(report.getNextActions().stream().anyMatch(action -> action.contains("覆盖率只包含 successful partial tables")));
        assertTrue(report.getNextActions().stream().noneMatch(action -> action.contains("secret")
                || action.contains("jdbc:postgresql")
                || action.contains("Authorization")));
        assertEquals(1, report.getSummary().getTableCount());
        assertEquals(1, report.getSummary().getColumnCount());
        assertEquals(1, report.getSummary().getCoveredCount());
        assertEquals(List.of("user_order"), report.getTables().stream().map(table -> table.getTableName()).toList());
    }

    private Field field(String name, String displayName, String dataType, String aliases) {
        Field field = new Field();
        field.setName(name);
        field.setDisplayName(displayName);
        field.setDataType(dataType);
        field.setAliases(aliases);
        field.setStatus("enabled");
        return field;
    }

    private ColumnDef column(String name, String dataType, String comment) {
        return ColumnDef.builder()
                .name(name)
                .dataType(dataType)
                .comment(comment)
                .nullable(true)
                .build();
    }

    private DatabaseSchemaTable schemaTable(String tableName, String columnName) {
        DatabaseSchemaTable table = new DatabaseSchemaTable();
        table.setTableName(tableName);
        DatabaseSchemaColumn column = new DatabaseSchemaColumn();
        column.setColumnName(columnName);
        column.setDataType("bigint");
        column.setComment("用户ID");
        column.setNullable(false);
        table.getColumns().add(column);
        return table;
    }
}
