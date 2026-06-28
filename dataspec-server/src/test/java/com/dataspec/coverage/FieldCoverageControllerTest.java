package com.dataspec.coverage;

import com.dataspec.coverage.controller.FieldCoverageController;
import com.dataspec.coverage.model.FieldCoverageReport;
import com.dataspec.coverage.service.FieldCoverageService;
import com.dataspec.reverseimport.model.DatabaseSchemaDumpReq;
import com.dataspec.reverseimport.service.DatabaseReverseImportService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FieldCoverageControllerTest {

    @Test
    void reportDump_delegatesToDatabaseService() {
        FieldCoverageService fieldCoverageService = mock(FieldCoverageService.class);
        DatabaseReverseImportService databaseService = mock(DatabaseReverseImportService.class);
        FieldCoverageController controller = new FieldCoverageController(fieldCoverageService, databaseService);
        DatabaseSchemaDumpReq req = new DatabaseSchemaDumpReq();
        FieldCoverageReport report = new FieldCoverageReport();
        when(databaseService.coverageDump(req)).thenReturn(report);

        assertThat(controller.reportDump(req).getData()).isSameAs(report);

        verify(databaseService).coverageDump(req);
    }
}
