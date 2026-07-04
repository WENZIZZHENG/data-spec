package com.dataspec.reverseimport;

import com.dataspec.field.entity.Field;
import com.dataspec.reverseimport.entity.FieldSource;
import com.dataspec.reverseimport.entity.ReverseImportDecision;
import com.dataspec.reverseimport.entity.ReverseImportBatch;
import com.dataspec.reverseimport.model.DatabaseImportReq;
import com.dataspec.reverseimport.model.FieldCandidate;
import com.dataspec.reverseimport.repository.FieldSourceRepository;
import com.dataspec.reverseimport.repository.ReverseImportDecisionRepository;
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
        ReverseImportDecisionRepository decisionRepository = mock(ReverseImportDecisionRepository.class);
        ReverseImportSourceServiceImpl service = new ReverseImportSourceServiceImpl(
                batchRepository,
                sourceRepository,
                decisionRepository,
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
        FieldCandidate candidate = new FieldCandidate(
                "user_order",
                "user_name",
                "varchar(50)",
                true,
                "password=source-secret",
                "用户名 jdbc:postgresql://db.internal:5432/app");
        candidate.setConfirmReason("不应进入来源快照");
        candidate.setIgnoreReason("不应进入来源快照");
        service.recordFieldSource(batch, field, candidate);

        ArgumentCaptor<FieldSource> sourceCaptor = ArgumentCaptor.forClass(FieldSource.class);
        verify(sourceRepository).insert(sourceCaptor.capture());
        assertThat(sourceCaptor.getValue().getFieldId()).isEqualTo(99L);
        assertThat(sourceCaptor.getValue().getBatchId()).isEqualTo(7L);
        assertThat(sourceCaptor.getValue().getColumnName()).isEqualTo("user_name");
        assertThat(sourceCaptor.getValue().getMetadataJson())
                .contains("user_name", "[REDACTED]")
                .doesNotContain(
                        "source-secret",
                        "jdbc:postgresql://db.internal:5432/app",
                        "confirmReason",
                        "ignoreReason",
                        "不应进入来源快照");
    }

    @Test
    void listByFieldId_returnsBatchSummaryAndSupportsNoSource() {
        ReverseImportBatchRepository batchRepository = mock(ReverseImportBatchRepository.class);
        FieldSourceRepository sourceRepository = mock(FieldSourceRepository.class);
        ReverseImportDecisionRepository decisionRepository = mock(ReverseImportDecisionRepository.class);
        ReverseImportSourceServiceImpl service = new ReverseImportSourceServiceImpl(
                batchRepository,
                sourceRepository,
                decisionRepository,
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

    @Test
    void recordAndListMappingDecisions_persistsBatchBoundDecisionHistory() {
        ReverseImportBatchRepository batchRepository = mock(ReverseImportBatchRepository.class);
        FieldSourceRepository sourceRepository = mock(FieldSourceRepository.class);
        ReverseImportDecisionRepository decisionRepository = mock(ReverseImportDecisionRepository.class);
        ReverseImportSourceServiceImpl service = new ReverseImportSourceServiceImpl(
                batchRepository,
                sourceRepository,
                decisionRepository,
                new ObjectMapper());
        ReverseImportBatch batch = new ReverseImportBatch();
        batch.setId(7L);
        batch.setProjectId(1L);
        batch.setSchemaName("public");
        ReverseImportDecision decision = new ReverseImportDecision();
        decision.setTableName("user_order");
        decision.setColumnName("mobile_no");
        decision.setDecisionType("IMPORTED");
        when(decisionRepository.findByBatchId(7L)).thenReturn(List.of(decision));

        service.recordMappingDecisions(batch, List.of(decision));

        ArgumentCaptor<ReverseImportDecision> decisionCaptor = ArgumentCaptor.forClass(ReverseImportDecision.class);
        verify(decisionRepository).insert(decisionCaptor.capture());
        assertThat(decisionCaptor.getValue().getProjectId()).isEqualTo(1L);
        assertThat(decisionCaptor.getValue().getBatchId()).isEqualTo(7L);
        assertThat(decisionCaptor.getValue().getSchemaName()).isEqualTo("public");

        assertThat(service.listDecisions(1L, 7L, 50)).containsExactly(decision);
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
