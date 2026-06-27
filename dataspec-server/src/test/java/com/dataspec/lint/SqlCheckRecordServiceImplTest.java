package com.dataspec.lint;

import com.dataspec.lint.entity.SqlCheckRecord;
import com.dataspec.lint.model.LintResult;
import com.dataspec.lint.model.SqlCheckReplay;
import com.dataspec.lint.repository.SqlCheckRecordRepository;
import com.dataspec.lint.service.impl.SqlCheckRecordServiceImpl;
import com.dataspec.standard.dto.StandardSnapshotInfo;
import com.dataspec.standard.dto.StandardSnapshotPayload;
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

    @Test
    void buildReplay_marksRecordAsCurrentWhenSnapshotHashMatches() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        StandardSnapshotService standardSnapshotService = mock(StandardSnapshotService.class);
        SqlCheckRecordServiceImpl service = new SqlCheckRecordServiceImpl(
                mock(SqlCheckRecordRepository.class),
                mapper,
                standardSnapshotService);
        SqlCheckRecord record = recordWithSnapshot("hash1");
        when(standardSnapshotService.getCurrentSnapshot(1L)).thenReturn(snapshotInfo("hash1"));
        when(standardSnapshotService.getSnapshotPayload(1L, 5L)).thenReturn(payload(mapper, "hash1"));

        SqlCheckReplay replay = service.buildReplay(record);

        assertEquals("current", replay.status());
        assertEquals("snapshot", replay.recordedStandard().source());
        assertEquals("current", replay.currentStandard().source());
        assertEquals(2, replay.summary().fieldCount());
        assertEquals(1, replay.summary().ruleCount());
        assertEquals(
                "dataspec export-context --project 1 --snapshot-id 5 --output dataspec-ai-context-snapshot-5.zip",
                replay.summary().exportCommand());
    }

    @Test
    void buildReplay_marksRecordAsHistoricalWhenCurrentHashDiffers() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        StandardSnapshotService standardSnapshotService = mock(StandardSnapshotService.class);
        SqlCheckRecordServiceImpl service = new SqlCheckRecordServiceImpl(
                mock(SqlCheckRecordRepository.class),
                mapper,
                standardSnapshotService);
        SqlCheckRecord record = recordWithSnapshot("old-hash");
        when(standardSnapshotService.getCurrentSnapshot(1L)).thenReturn(snapshotInfo("new-hash"));
        when(standardSnapshotService.getSnapshotPayload(1L, 5L)).thenReturn(payload(mapper, "old-hash"));

        SqlCheckReplay replay = service.buildReplay(record);

        assertEquals("historical", replay.status());
        assertEquals("old-hash", replay.recordedStandard().specHash());
        assertEquals("new-hash", replay.currentStandard().specHash());
        assertEquals(false, replay.summary().sameAsCurrent());
        assertEquals(2, replay.nextActions().size());
    }

    @Test
    void buildReplay_marksUnversionedRecordWithoutSnapshotReference() {
        StandardSnapshotService standardSnapshotService = mock(StandardSnapshotService.class);
        SqlCheckRecordServiceImpl service = new SqlCheckRecordServiceImpl(
                mock(SqlCheckRecordRepository.class),
                new ObjectMapper(),
                standardSnapshotService);
        SqlCheckRecord record = new SqlCheckRecord();
        record.setProjectId(1L);
        when(standardSnapshotService.getCurrentSnapshot(1L)).thenReturn(snapshotInfo("current-hash"));

        SqlCheckReplay replay = service.buildReplay(record);

        assertEquals("unversioned", replay.status());
        assertEquals("unversioned", replay.recordedStandard().source());
        assertEquals("current", replay.currentStandard().source());
        assertEquals(1, replay.nextActions().size());
    }

    private SqlCheckRecord recordWithSnapshot(String hash) {
        SqlCheckRecord record = new SqlCheckRecord();
        record.setProjectId(1L);
        record.setStandardSnapshotId(5L);
        record.setStandardSnapshotVersion("v1");
        record.setStandardSnapshotHash(hash);
        return record;
    }

    private StandardSnapshotInfo snapshotInfo(String hash) {
        return new StandardSnapshotInfo(5L, 1L, "v1", "P6-1", null, hash, null, true);
    }

    private StandardSnapshotPayload payload(ObjectMapper mapper, String hash) throws Exception {
        return new StandardSnapshotPayload(
                new StandardSnapshotInfo(5L, 1L, "v1", "P6-1", null, hash, null, true, "snapshot"),
                mapper.readTree("""
                        {"projectId":1,"fields":[{"name":"id"},{"name":"created_at"}],"enums":[],"rules":[{"ruleCode":"field_naming_snake_case"}]}
                        """),
                2,
                0,
                1);
    }
}
