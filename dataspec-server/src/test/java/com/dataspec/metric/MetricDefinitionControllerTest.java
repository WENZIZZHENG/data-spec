package com.dataspec.metric;

import com.dataspec.metric.controller.MetricDefinitionController;
import com.dataspec.metric.model.MetricDefinitionReq;
import com.dataspec.metric.model.MetricDefinitionResp;
import com.dataspec.metric.service.MetricDefinitionService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MetricDefinitionControllerTest {

    @Test
    void list_forwardsFiltersAndWrapsR() {
        MetricDefinitionService service = mock(MetricDefinitionService.class);
        MetricDefinitionResp item = new MetricDefinitionResp(
                1L, 1L, "order_amount", "订单金额", "支付成功订单金额",
                List.of(10L), List.of(11L), "status='PAID'", "sum(amount_cent)",
                "day", "只读说明", "SELECT sum(amount_cent) FROM orders",
                List.of(), "enabled", null, null);
        when(service.list(1L, "order", "enabled", 10L, "order_amount", 5)).thenReturn(List.of(item));
        MetricDefinitionController controller = new MetricDefinitionController(service);

        var response = controller.list(1L, "order", "enabled", 10L, "order_amount", 5);

        assertThat(response.getCode()).isEqualTo(200);
        assertThat(response.getData()).containsExactly(item);
        verify(service).list(1L, "order", "enabled", 10L, "order_amount", 5);
    }

    @Test
    void create_forwardsRequestAndWrapsR() {
        MetricDefinitionService service = mock(MetricDefinitionService.class);
        MetricDefinitionReq req = new MetricDefinitionReq(
                1L, "order_amount", "订单金额", "支付成功订单金额",
                List.of(10L), List.of(), null, "sum(amount_cent)", "day",
                "只读说明", "SELECT sum(amount_cent) FROM orders", List.of(), "enabled");
        MetricDefinitionResp created = new MetricDefinitionResp(
                2L, 1L, "order_amount", "订单金额", "支付成功订单金额",
                List.of(10L), List.of(), null, "sum(amount_cent)", "day",
                "只读说明", "SELECT sum(amount_cent) FROM orders", List.of(), "enabled", null, null);
        when(service.create(req)).thenReturn(created);
        MetricDefinitionController controller = new MetricDefinitionController(service);

        var response = controller.create(req);

        assertThat(response.getData()).isSameAs(created);
        verify(service).create(req);
    }
}
