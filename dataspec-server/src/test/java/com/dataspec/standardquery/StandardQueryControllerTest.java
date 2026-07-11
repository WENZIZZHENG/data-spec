package com.dataspec.standardquery;

import com.dataspec.standardquery.exception.StandardQueryValidationException;
import com.dataspec.standardquery.controller.StandardQueryController;
import com.dataspec.standardquery.model.StandardQueryNormalized;
import com.dataspec.standardquery.model.StandardQueryRequest;
import com.dataspec.standardquery.model.StandardQueryResult;
import com.dataspec.standardquery.model.StandardQuerySummary;
import com.dataspec.standardquery.model.StandardQueryValidationError;
import com.dataspec.standardquery.service.StandardQueryService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Standard Query DSL API 测试。
 */
class StandardQueryControllerTest {

    @Test
    void search_forwardsReadOnlyDslRequestToService() {
        StandardQueryService service = mock(StandardQueryService.class);
        StandardQueryRequest request = new StandardQueryRequest(
                1L, "FIELD", "订单金额", List.of(), null, 10, true, false);
        StandardQueryResult result = new StandardQueryResult(
                1L,
                new StandardQueryNormalized("FIELD", "订单金额", List.of(), List.of(), 10, true, false),
                new StandardQuerySummary("FIELD", "订单金额", 0, 0, false, List.of()),
                List.of(),
                List.of(),
                0,
                0,
                false,
                List.of(),
                List.of());
        when(service.search(request)).thenReturn(result);
        StandardQueryController controller = new StandardQueryController(service);

        var response = controller.search(request);

        assertEquals("FIELD", response.getData().normalizedQuery().target());
        verify(service).search(request);
    }

    @Test
    void handleValidation_returnsDslValidationErrorPayload() {
        StandardQueryController controller = new StandardQueryController(mock(StandardQueryService.class));
        StandardQueryValidationError error = new StandardQueryValidationError(
                "STANDARD_QUERY_DSL_INVALID",
                "Standard Query limit 必须在 1 到 50 之间",
                List.of("category", "stableRef"),
                List.of("eq"),
                "limit=1..50");

        var response = controller.handleValidation(new StandardQueryValidationException(error));

        assertEquals(400, response.getBody().getCode());
        assertEquals("STANDARD_QUERY_DSL_INVALID", response.getBody().getData().code());
        assertEquals("limit=1..50", response.getBody().getData().bounds());
        assertEquals("STANDARD_QUERY_DSL_INVALID", response.getBody().getError().code());
        assertEquals("VALIDATION", response.getBody().getError().category());
        assertEquals("standard-query-dsl", response.getBody().getError().capabilityId());
    }
}
