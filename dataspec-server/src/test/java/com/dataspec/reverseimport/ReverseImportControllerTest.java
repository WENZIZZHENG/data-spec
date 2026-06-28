package com.dataspec.reverseimport;

import com.dataspec.reverseimport.controller.ReverseImportController;
import com.dataspec.reverseimport.model.DatabaseConnectionReq;
import com.dataspec.reverseimport.model.DatabaseSchemaDump;
import com.dataspec.reverseimport.model.DatabaseSchemaDumpReq;
import com.dataspec.reverseimport.model.ReverseImportCompareResult;
import com.dataspec.reverseimport.model.ReverseImportPreview;
import com.dataspec.reverseimport.service.DatabaseReverseImportService;
import com.dataspec.reverseimport.service.ReverseImportService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReverseImportControllerTest {

    @Test
    void dumpEndpoints_delegateToDatabaseService() {
        ReverseImportService reverseImportService = mock(ReverseImportService.class);
        DatabaseReverseImportService databaseService = mock(DatabaseReverseImportService.class);
        ReverseImportController controller = new ReverseImportController(reverseImportService, databaseService);
        DatabaseConnectionReq connectionReq = new DatabaseConnectionReq();
        DatabaseSchemaDump dump = new DatabaseSchemaDump();
        DatabaseSchemaDumpReq dumpReq = new DatabaseSchemaDumpReq();
        ReverseImportPreview preview = new ReverseImportPreview();
        ReverseImportCompareResult compare = new ReverseImportCompareResult();
        when(databaseService.exportDump(connectionReq)).thenReturn(dump);
        when(databaseService.previewDump(dumpReq)).thenReturn(preview);
        when(databaseService.compareDump(dumpReq)).thenReturn(compare);

        assertThat(controller.exportDatabaseDump(connectionReq).getData()).isSameAs(dump);
        assertThat(controller.previewDump(dumpReq).getData()).isSameAs(preview);
        assertThat(controller.compareDump(dumpReq).getData()).isSameAs(compare);

        verify(databaseService).exportDump(connectionReq);
        verify(databaseService).previewDump(dumpReq);
        verify(databaseService).compareDump(dumpReq);
    }
}
