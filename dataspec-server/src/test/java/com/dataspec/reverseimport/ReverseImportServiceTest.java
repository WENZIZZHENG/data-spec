package com.dataspec.reverseimport;

import com.dataspec.field.entity.Field;
import com.dataspec.field.service.FieldService;
import com.dataspec.lint.engine.SqlParserService;
import com.dataspec.lint.model.ColumnDef;
import com.dataspec.lint.model.TableDef;
import com.dataspec.reverseimport.model.ReverseImportCompareResult;
import com.dataspec.reverseimport.model.ReverseImportFieldStatus;
import com.dataspec.reverseimport.model.ReverseImportPreview;
import com.dataspec.reverseimport.service.impl.ReverseImportServiceImpl;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * SQL 反向导入预览测试。
 */
class ReverseImportServiceTest {

    @Test
    void preview_returnsCandidatesMissingCommentsAndAliasMatches() {
        FieldService fieldService = mock(FieldService.class);
        when(fieldService.listByProject(1L)).thenReturn(List.of(
                standardField("id", null),
                standardField("mobile_no", "phone,mobile")
        ));
        ReverseImportServiceImpl service = new ReverseImportServiceImpl(new SqlParserService(), fieldService);

        String sql = """
                CREATE TABLE user_order (
                    id bigint NOT NULL,
                    phone varchar(20),
                    user_name varchar(50)
                );
                COMMENT ON TABLE user_order IS '用户订单';
                COMMENT ON COLUMN user_order.phone IS '手机号';
                """;

        ReverseImportPreview preview = service.preview(1L, sql);

        assertThat(preview.getSummary().getTableCount()).isEqualTo(1);
        assertThat(preview.getSummary().getColumnCount()).isEqualTo(3);
        assertThat(preview.getFieldCandidates()).extracting("columnName").containsExactly("user_name");
        assertThat(preview.getMissingComments()).extracting("columnName").containsExactlyInAnyOrder("id", "user_name");
        assertThat(preview.getNonStandardFields()).extracting("columnName").containsExactly("user_name");
    }

    @Test
    void compareTables_reportsAliasMatchChangesNewFieldsAndMissingComments() {
        FieldService fieldService = mock(FieldService.class);
        Field id = standardField("id", null);
        id.setDataType("bigint");
        id.setNullable(false);
        id.setComment("主键");
        Field mobileNo = standardField("mobile_no", "phone,mobile");
        mobileNo.setDisplayName("手机号");
        mobileNo.setDataType("varchar");
        mobileNo.setLength(20);
        mobileNo.setNullable(true);
        mobileNo.setComment("手机号");
        Field amount = standardField("amount_cent", null);
        amount.setDataType("bigint");
        amount.setNullable(false);
        amount.setDefaultValue("0");
        amount.setComment("金额分");
        Field createdAt = standardField("created_at", null);
        createdAt.setDataType("timestamp");
        createdAt.setNullable(true);
        createdAt.setComment("创建时间");
        when(fieldService.listByProject(1L)).thenReturn(List.of(id, mobileNo, amount, createdAt));
        ReverseImportServiceImpl service = new ReverseImportServiceImpl(new SqlParserService(), fieldService);

        TableDef table = TableDef.builder()
                .name("user_order")
                .comment("用户订单")
                .columns(List.of(
                        column("id", "BIGINT", false, null, "主键"),
                        column("phone", "VARCHAR(20)", true, null, "手机号"),
                        column("amount_cent", "BIGINT", true, null, ""),
                        column("created_at", "TIMESTAMP", true, null, ""),
                        column("user_name", "VARCHAR(50)", true, null, "用户名")
                ))
                .build();

        ReverseImportCompareResult result = service.compareTables(1L, List.of(table));

        assertThat(result.getSummary().getTableCount()).isEqualTo(1);
        assertThat(result.getSummary().getColumnCount()).isEqualTo(5);
        assertThat(result.getSummary().getMatchedCount()).isEqualTo(4);
        assertThat(result.getSummary().getChangedCount()).isEqualTo(1);
        assertThat(result.getSummary().getNewCount()).isEqualTo(1);
        assertThat(result.getSummary().getMissingCommentCount()).isEqualTo(2);
        assertThat(result.getSummary().getNonStandardCount()).isEqualTo(1);
        assertThat(result.getTableDiffs()).hasSize(1);
        assertThat(result.getTableDiffs().get(0).getFieldDiffs())
                .extracting("columnName", "status")
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("id", ReverseImportFieldStatus.MATCHED),
                        org.assertj.core.groups.Tuple.tuple("phone", ReverseImportFieldStatus.MATCHED),
                        org.assertj.core.groups.Tuple.tuple("amount_cent", ReverseImportFieldStatus.CHANGED),
                        org.assertj.core.groups.Tuple.tuple("created_at", ReverseImportFieldStatus.MISSING_COMMENT),
                        org.assertj.core.groups.Tuple.tuple("user_name", ReverseImportFieldStatus.NEW)
                );
        assertThat(result.getTableDiffs().get(0).getFieldDiffs().get(1).getStandardFieldName())
                .isEqualTo("mobile_no");
        assertThat(result.getTableDiffs().get(0).getFieldDiffs().get(2).getChanges())
                .extracting("property")
                .containsExactly("nullable", "defaultValue", "comment");
        assertThat(result.getTableDiffs().get(0).getFieldDiffs())
                .filteredOn(diff -> "user_name".equals(diff.getColumnName()))
                .singleElement()
                .extracting("nonStandard")
                .isEqualTo(true);
    }

    private ColumnDef column(String name, String dataType, boolean nullable, String defaultValue, String comment) {
        return ColumnDef.builder()
                .name(name)
                .dataType(dataType)
                .nullable(nullable)
                .defaultValue(defaultValue)
                .comment(comment)
                .build();
    }

    private Field standardField(String name, String aliases) {
        Field field = new Field();
        field.setProjectId(1L);
        field.setName(name);
        field.setAliases(aliases);
        return field;
    }
}
