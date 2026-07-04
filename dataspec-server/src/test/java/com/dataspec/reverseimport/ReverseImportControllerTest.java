package com.dataspec.reverseimport;

import com.dataspec.reverseimport.controller.ReverseImportController;
import com.dataspec.reverseimport.entity.ReverseImportDecision;
import com.dataspec.reverseimport.model.DatabaseConnectionReq;
import com.dataspec.reverseimport.model.DatabaseSchemaDump;
import com.dataspec.reverseimport.model.DatabaseSchemaDumpReq;
import com.dataspec.reverseimport.model.ReverseImportCompareResult;
import com.dataspec.reverseimport.model.ReverseImportPreview;
import com.dataspec.reverseimport.service.DatabaseReverseImportService;
import com.dataspec.reverseimport.service.ReverseImportService;
import com.dataspec.reverseimport.service.ReverseImportSourceService;
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
        ReverseImportSourceService sourceService = mock(ReverseImportSourceService.class);
        ReverseImportController controller = new ReverseImportController(reverseImportService, databaseService, sourceService);
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

    @Test
    void listMappingDecisions_delegatesToSourceService() {
        ReverseImportService reverseImportService = mock(ReverseImportService.class);
        DatabaseReverseImportService databaseService = mock(DatabaseReverseImportService.class);
        ReverseImportSourceService sourceService = mock(ReverseImportSourceService.class);
        ReverseImportController controller = new ReverseImportController(reverseImportService, databaseService, sourceService);
        ReverseImportDecision decision = new ReverseImportDecision();
        decision.setColumnName("mobile_no");
        when(sourceService.listDecisions(1L, 7L, 20)).thenReturn(java.util.List.of(decision));

        var result = controller.listMappingDecisions(1L, 7L, 20);

        assertThat(result.getData()).containsExactly(decision);
        verify(sourceService).listDecisions(1L, 7L, 20);
    }
}
