package com.dataspec.coverage;

import com.dataspec.coverage.controller.FieldCoverageController;
import com.dataspec.coverage.model.FieldCoverageReport;
import com.dataspec.coverage.service.FieldCoverageService;
import com.dataspec.reverseimport.model.DatabaseMetadataScanFailureSummary;
import com.dataspec.reverseimport.model.DatabaseMetadataScanPartialResult;
import com.dataspec.reverseimport.model.DatabaseSchemaDumpReq;
import com.dataspec.reverseimport.service.DatabaseReverseImportService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
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

    @Test
    void reportScanPartial_delegatesToCoverageService() {
        FieldCoverageService fieldCoverageService = mock(FieldCoverageService.class);
        DatabaseReverseImportService databaseService = mock(DatabaseReverseImportService.class);
        FieldCoverageController controller = new FieldCoverageController(fieldCoverageService, databaseService);
        DatabaseMetadataScanPartialResult partial = new DatabaseMetadataScanPartialResult();
        DatabaseMetadataScanFailureSummary failureSummary = new DatabaseMetadataScanFailureSummary();
        FieldCoverageController.ScanPartialCoverageReq req = new FieldCoverageController.ScanPartialCoverageReq(
                1L,
                partial,
                failureSummary,
                "PARTIAL");
        FieldCoverageReport report = new FieldCoverageReport();
        when(fieldCoverageService.reportScanPartial(eq(1L), eq(partial), eq(failureSummary), eq("PARTIAL"))).thenReturn(report);

        assertThat(controller.reportScanPartial(req).getData()).isSameAs(report);

        verify(fieldCoverageService).reportScanPartial(1L, partial, failureSummary, "PARTIAL");
    }
}
