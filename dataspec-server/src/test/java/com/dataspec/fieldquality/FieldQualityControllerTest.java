package com.dataspec.fieldquality;

import com.dataspec.fieldquality.controller.FieldQualityController;
import com.dataspec.fieldquality.model.FieldQualityReport;
import com.dataspec.fieldquality.service.FieldQualityService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FieldQualityControllerTest {

    @Test
    void report_returnsServiceResult() {
        FieldQualityService service = mock(FieldQualityService.class);
        FieldQualityReport report = new FieldQualityReport();
        when(service.report(1L)).thenReturn(report);
        FieldQualityController controller = new FieldQualityController(service);

        var response = controller.report(1L);

        assertSame(report, response.getData());
        verify(service).report(1L);
    }
}
