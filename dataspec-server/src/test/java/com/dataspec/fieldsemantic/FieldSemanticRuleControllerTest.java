package com.dataspec.fieldsemantic;

import com.dataspec.fieldsemantic.controller.FieldSemanticRuleController;
import com.dataspec.fieldsemantic.model.FieldSemanticRuleReq;
import com.dataspec.fieldsemantic.model.FieldSemanticRuleResp;
import com.dataspec.fieldsemantic.service.FieldSemanticRuleService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FieldSemanticRuleControllerTest {

    @Test
    void list_forwardsFiltersAndWrapsR() {
        FieldSemanticRuleService service = mock(FieldSemanticRuleService.class);
        FieldSemanticRuleResp item = new FieldSemanticRuleResp(
                1L, 1L, 10L, 11L, "DERIVED_FROM", null, null, null,
                "以源字段为准", null, null, List.of(), "enabled", null, null);
        when(service.list(1L, 10L, "derived-from", "金额", 5)).thenReturn(List.of(item));
        FieldSemanticRuleController controller = new FieldSemanticRuleController(service);

        var response = controller.list(1L, 10L, "derived-from", "金额", 5);

        assertThat(response.getCode()).isEqualTo(200);
        assertThat(response.getData()).containsExactly(item);
        verify(service).list(1L, 10L, "derived-from", "金额", 5);
    }

    @Test
    void create_forwardsRequestAndWrapsR() {
        FieldSemanticRuleService service = mock(FieldSemanticRuleService.class);
        FieldSemanticRuleReq req = new FieldSemanticRuleReq(
                1L, 10L, null, "AGGREGATION", null, "sum(amount_cent)",
                null, null, null, null, List.of(), "enabled");
        FieldSemanticRuleResp created = new FieldSemanticRuleResp(
                2L, 1L, 10L, null, "AGGREGATION", null, "sum(amount_cent)",
                null, null, null, null, List.of(), "enabled", null, null);
        when(service.create(req)).thenReturn(created);
        FieldSemanticRuleController controller = new FieldSemanticRuleController(service);

        var response = controller.create(req);

        assertThat(response.getData()).isSameAs(created);
        verify(service).create(req);
    }
}
