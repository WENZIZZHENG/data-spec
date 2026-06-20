package com.dataspec.reverseimport;

import com.dataspec.field.entity.Field;
import com.dataspec.reverseimport.entity.FieldSource;
import com.dataspec.reverseimport.entity.ReverseImportBatch;
import com.dataspec.reverseimport.model.DatabaseImportReq;
import com.dataspec.reverseimport.model.FieldCandidate;
import com.dataspec.reverseimport.repository.FieldSourceRepository;
import com.dataspec.reverseimport.repository.ReverseImportBatchRepository;
import com.dataspec.reverseimport.service.impl.ReverseImportSourceServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

/**
 * 数据库反向导入来源追踪测试。
 */
class ReverseImportSourceServiceTest {

    @Test
    void createBatchAndRecordFieldSource_persistsNonSensitiveSourceMetadata() {
        ReverseImportBatchRepository batchRepository = mock(ReverseImportBatchRepository.class);
        FieldSourceRepository sourceRepository = mock(FieldSourceRepository.class);
        ReverseImportSourceServiceImpl service = new ReverseImportSourceServiceImpl(
                batchRepository,
                sourceRepository,
                new ObjectMapper());
        DatabaseImportReq req = importReq();

        ReverseImportBatch batch = service.createDatabaseBatch(req, 1, 2);

        ArgumentCaptor<ReverseImportBatch> batchCaptor = ArgumentCaptor.forClass(ReverseImportBatch.class);
        verify(batchRepository).insert(batchCaptor.capture());
        assertThat(batchCaptor.getValue().getDatabaseType()).isEqualTo("postgresql");
        assertThat(batchCaptor.getValue().getDatabaseName()).isEqualTo("demo");
        assertThat(batchCaptor.getValue().getTableNamesJson()).contains("user_order");
        assertThat(batchCaptor.getValue().getImportedCount()).isEqualTo(1);
        assertThat(batchCaptor.getValue().getSkippedCount()).isEqualTo(2);

        batch.setId(7L);
        Field field = new Field();
        field.setId(99L);
        field.setProjectId(1L);
        FieldCandidate candidate = new FieldCandidate("user_order", "user_name", "varchar(50)", true, null, "用户名");
        service.recordFieldSource(batch, field, candidate);

        ArgumentCaptor<FieldSource> sourceCaptor = ArgumentCaptor.forClass(FieldSource.class);
        verify(sourceRepository).insert(sourceCaptor.capture());
        assertThat(sourceCaptor.getValue().getFieldId()).isEqualTo(99L);
        assertThat(sourceCaptor.getValue().getBatchId()).isEqualTo(7L);
        assertThat(sourceCaptor.getValue().getColumnName()).isEqualTo("user_name");
        assertThat(sourceCaptor.getValue().getMetadataJson()).contains("user_name");
    }

    @Test
    void listByFieldId_returnsBatchSummaryAndSupportsNoSource() {
        ReverseImportBatchRepository batchRepository = mock(ReverseImportBatchRepository.class);
        FieldSourceRepository sourceRepository = mock(FieldSourceRepository.class);
        ReverseImportSourceServiceImpl service = new ReverseImportSourceServiceImpl(
                batchRepository,
                sourceRepository,
                new ObjectMapper());
        FieldSource source = new FieldSource();
        source.setFieldId(99L);
        source.setBatchId(7L);
        ReverseImportBatch batch = new ReverseImportBatch();
        batch.setId(7L);
        when(sourceRepository.findByFieldId(99L)).thenReturn(List.of(source));
        when(batchRepository.findById(7L)).thenReturn(Optional.of(batch));
        when(sourceRepository.findByFieldId(100L)).thenReturn(List.of());

        var details = service.listByFieldId(99L);

        assertThat(details).hasSize(1);
        assertThat(details.getFirst().source()).isSameAs(source);
        assertThat(details.getFirst().batch()).isSameAs(batch);
        assertThat(service.listByFieldId(100L)).isEmpty();
    }

    private DatabaseImportReq importReq() {
        DatabaseImportReq req = new DatabaseImportReq();
        req.setProjectId(1L);
        req.setDatabaseType("postgresql");
        req.setDatabaseName("demo");
        req.setSchemaName("public");
        req.setTableNames(List.of("user_order"));
        return req;
    }
}
