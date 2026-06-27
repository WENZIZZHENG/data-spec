package com.dataspec.fieldconflict;

import com.dataspec.fieldconflict.controller.FieldConflictController;
import com.dataspec.fieldconflict.model.FieldConflictReport;
import com.dataspec.fieldconflict.service.FieldConflictService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FieldConflictControllerTest {

    @Test
    void report_returnsFieldConflictReport() {
        FieldConflictService service = mock(FieldConflictService.class);
        FieldConflictReport report = new FieldConflictReport();
        report.setProjectId(1L);
        when(service.report(1L)).thenReturn(report);
        FieldConflictController controller = new FieldConflictController(service);

        var response = controller.report(1L);

        assertSame(report, response.getData());
    }
}
