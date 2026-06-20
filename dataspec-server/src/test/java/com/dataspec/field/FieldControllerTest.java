package com.dataspec.field;

import com.dataspec.field.controller.FieldController;
import com.dataspec.field.entity.Field;
import com.dataspec.field.model.FieldSuggestion;
import com.dataspec.field.service.FieldService;
import com.dataspec.reverseimport.entity.FieldSource;
import com.dataspec.reverseimport.model.FieldSourceDetail;
import com.dataspec.reverseimport.service.ReverseImportSourceService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 标准字段接口测试
 */
class FieldControllerTest {

    @Test
    void suggest_returnsServiceSuggestions() {
        FieldService service = mock(FieldService.class);
        Field field = new Field();
        field.setName("mobile_no");
        when(service.suggest(1L, "用户手机号", 3))
                .thenReturn(List.of(new FieldSuggestion(field, 92, "显示名匹配", "mobile_no", true)));
        FieldController controller = new FieldController(service, mock(ReverseImportSourceService.class));

        var response = controller.suggest(1L, "用户手机号", 3);

        assertEquals(1, response.getData().size());
        assertEquals("mobile_no", response.getData().getFirst().recommendedName());
    }

    @Test
    void listSources_checksFieldAccessBeforeReturningSourceDetails() {
        FieldService service = mock(FieldService.class);
        ReverseImportSourceService sourceService = mock(ReverseImportSourceService.class);
        Field field = new Field();
        field.setId(99L);
        field.setProjectId(1L);
        FieldSource source = new FieldSource();
        source.setFieldId(99L);
        when(service.getById(99L)).thenReturn(field);
        when(sourceService.listByFieldId(99L)).thenReturn(List.of(new FieldSourceDetail(source, null)));
        FieldController controller = new FieldController(service, sourceService);

        var response = controller.listSources(99L);

        assertEquals(1, response.getData().size());
        verify(service).getById(99L);
        verify(sourceService).listByFieldId(99L);
    }
}
