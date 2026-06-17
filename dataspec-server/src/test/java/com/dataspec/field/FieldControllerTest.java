package com.dataspec.field;

import com.dataspec.field.controller.FieldController;
import com.dataspec.field.entity.Field;
import com.dataspec.field.model.FieldSuggestion;
import com.dataspec.field.service.FieldService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
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
        FieldController controller = new FieldController(service);

        var response = controller.suggest(1L, "用户手机号", 3);

        assertEquals(1, response.getData().size());
        assertEquals("mobile_no", response.getData().getFirst().recommendedName());
    }
}
