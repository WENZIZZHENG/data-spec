package com.dataspec.lint;

import com.dataspec.lint.entity.SqlCheckRecord;
import com.dataspec.lint.model.LintResult;
import com.dataspec.lint.repository.SqlCheckRecordRepository;
import com.dataspec.lint.service.impl.SqlCheckRecordServiceImpl;
import com.dataspec.standard.dto.StandardSnapshotInfo;
import com.dataspec.standard.service.StandardSnapshotService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class SqlCheckRecordServiceImplTest {

    @Test
    void save_recordsCurrentStandardSnapshotReference() {
        SqlCheckRecordRepository repository = mock(SqlCheckRecordRepository.class);
        StandardSnapshotService standardSnapshotService = mock(StandardSnapshotService.class);
        SqlCheckRecordServiceImpl service = new SqlCheckRecordServiceImpl(
                repository,
                new ObjectMapper(),
                standardSnapshotService);
        when(standardSnapshotService.getCurrentSnapshot(1L)).thenReturn(new StandardSnapshotInfo(
                5L,
                1L,
                "v2026.06.24",
                "P6-1",
                null,
                "abc123",
                null,
                true));

        service.save(1L, "CREATE TABLE users(id bigint);", LintResult.of(List.of(), List.of()));

        ArgumentCaptor<SqlCheckRecord> captor = ArgumentCaptor.forClass(SqlCheckRecord.class);
        verify(repository).insert(captor.capture());
        SqlCheckRecord record = captor.getValue();
        assertEquals(5L, record.getStandardSnapshotId());
        assertEquals("v2026.06.24", record.getStandardSnapshotVersion());
        assertEquals("abc123", record.getStandardSnapshotHash());
    }
}
