package com.dataspec.fieldimpact;

import com.dataspec.fieldimpact.controller.FieldImpactController;
import com.dataspec.fieldimpact.model.FieldImpactReport;
import com.dataspec.fieldimpact.service.FieldImpactService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FieldImpactControllerTest {

    @Test
    void report_returnsServiceResult() {
        FieldImpactService service = mock(FieldImpactService.class);
        FieldImpactReport report = new FieldImpactReport();
        when(service.report(1L, 10L)).thenReturn(report);
        FieldImpactController controller = new FieldImpactController(service);

        var response = controller.report(10L, 1L);

        assertSame(report, response.getData());
        verify(service).report(1L, 10L);
    }
}
