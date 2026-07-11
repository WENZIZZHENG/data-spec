package com.dataspec.fieldknowledge;

import com.dataspec.fieldknowledge.controller.FieldKnowledgeCardController;
import com.dataspec.fieldknowledge.model.FieldKnowledgeCardListResp;
import com.dataspec.fieldknowledge.model.FieldKnowledgeCardResp;
import com.dataspec.fieldknowledge.service.FieldKnowledgeCardService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FieldKnowledgeCardControllerTest {

    @Test
    void list_forwardsFiltersAndWrapsR() {
        FieldKnowledgeCardService service = mock(FieldKnowledgeCardService.class);
        FieldKnowledgeCardListResp list = new FieldKnowledgeCardListResp(1L, 1, 1, false, List.of());
        when(service.list(1L, "amount", "enabled", 10L, 5)).thenReturn(list);
        FieldKnowledgeCardController controller = new FieldKnowledgeCardController(service);

        var response = controller.list(1L, "amount", "enabled", 10L, 5);

        assertThat(response.getCode()).isEqualTo(200);
        assertThat(response.getData()).isSameAs(list);
        verify(service).list(1L, "amount", "enabled", 10L, 5);
    }

    @Test
    void get_forwardsProjectAndFieldIdAndWrapsR() {
        FieldKnowledgeCardService service = mock(FieldKnowledgeCardService.class);
        FieldKnowledgeCardResp card = new FieldKnowledgeCardResp(
                1L, 10L, "field:1:10", "amount_cent", "订单金额", "bigint",
                "enabled", List.of(), List.of(), List.of(), List.of(), List.of(),
                List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), null);
        when(service.get(1L, 10L)).thenReturn(card);
        FieldKnowledgeCardController controller = new FieldKnowledgeCardController(service);

        var response = controller.get(10L, 1L);

        assertThat(response.getData()).isSameAs(card);
        verify(service).get(1L, 10L);
    }
}
