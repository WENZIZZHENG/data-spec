package com.dataspec.reverseimport;

import com.dataspec.field.entity.Field;
import com.dataspec.field.service.FieldService;
import com.dataspec.lint.engine.SqlParserService;
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

    private Field standardField(String name, String aliases) {
        Field field = new Field();
        field.setProjectId(1L);
        field.setName(name);
        field.setAliases(aliases);
        return field;
    }
}
