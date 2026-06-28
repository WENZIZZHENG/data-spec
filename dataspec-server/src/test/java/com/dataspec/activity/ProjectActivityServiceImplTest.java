package com.dataspec.activity;

import com.dataspec.activity.model.ProjectActivityItem;
import com.dataspec.activity.model.ProjectActivityTimeline;
import com.dataspec.activity.service.impl.ProjectActivityServiceImpl;
import com.dataspec.aireplay.entity.AiJobRecord;
import com.dataspec.aireplay.repository.AiJobRecordRepository;
import com.dataspec.changelog.entity.StandardChangeLog;
import com.dataspec.changelog.repository.StandardChangeLogRepository;
import com.dataspec.common.exception.BizException;
import com.dataspec.lint.entity.SqlCheckRecord;
import com.dataspec.lint.repository.SqlCheckRecordRepository;
import com.dataspec.reverseimport.entity.ReverseImportBatch;
import com.dataspec.reverseimport.repository.ReverseImportBatchRepository;
import com.dataspec.security.context.DataSpecSecurityContext;
import com.dataspec.security.entity.ApiToken;
import com.dataspec.security.model.ApiTokenPrincipal;
import com.dataspec.security.repository.ApiTokenRepository;
import com.dataspec.standard.entity.StandardSnapshot;
import com.dataspec.standard.repository.StandardSnapshotRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProjectActivityServiceImplTest {

    @AfterEach
    void tearDown() {
        DataSpecSecurityContext.clear();
    }

    @Test
    void listActivities_aggregatesSourcesSortedAndSanitized() {
        DataSpecSecurityContext.set(ApiTokenPrincipal.local());
        TestFixture fixture = new TestFixture();
        when(fixture.changeLogRepository.findByProjectId(1L, 100)).thenReturn(List.of(changeLog()));
        when(fixture.snapshotRepository.findRecentByProjectId(1L, 100)).thenReturn(List.of(snapshot()));
        when(fixture.reverseImportBatchRepository.findRecentByProjectId(1L, 100)).thenReturn(List.of(reverseImportBatch()));
        when(fixture.sqlCheckRecordRepository.findRecentByProjectId(1L, 100)).thenReturn(List.of(sqlCheckRecord()));
        when(fixture.aiJobRecordRepository.findRecentByProjectId(1L, 100)).thenReturn(List.of(aiJobRecord()));
        when(fixture.apiTokenRepository.findAllActiveRows()).thenReturn(List.of(
                token(601L, "*", LocalDateTime.of(2026, 6, 28, 10, 35)),
                token(602L, "2", LocalDateTime.of(2026, 6, 28, 10, 40))
        ));

        ProjectActivityTimeline timeline = fixture.service().listActivities(1L, null, 20);

        assertThat(timeline.projectId()).isEqualTo(1L);
        assertThat(timeline.availableActionTypes()).extracting("actionType")
                .containsExactly(
                        ProjectActivityServiceImpl.FIELD_CHANGE,
                        ProjectActivityServiceImpl.STANDARD_SNAPSHOT,
                        ProjectActivityServiceImpl.REVERSE_IMPORT,
                        ProjectActivityServiceImpl.SQL_CHECK,
                        ProjectActivityServiceImpl.AI_JOB,
                        ProjectActivityServiceImpl.TOKEN_USAGE);
        assertThat(timeline.activities()).extracting(ProjectActivityItem::actionType)
                .containsExactly(
                        ProjectActivityServiceImpl.TOKEN_USAGE,
                        ProjectActivityServiceImpl.AI_JOB,
                        ProjectActivityServiceImpl.SQL_CHECK,
                        ProjectActivityServiceImpl.REVERSE_IMPORT,
                        ProjectActivityServiceImpl.STANDARD_SNAPSHOT,
                        ProjectActivityServiceImpl.FIELD_CHANGE);
        assertThat(timeline.activities().get(1).severity()).isEqualTo("ERROR");
        assertThat(timeline.activities().get(2).severity()).isEqualTo("ERROR");
        assertThat(timeline.activities().get(2).metadata())
                .containsEntry("recordId", 301L)
                .containsEntry("issueCount", 2);
        assertThat(timeline.toString())
                .doesNotContain("select * from users", "CREATE TABLE fixed", "hash-secret", "ds_secret");
        assertThat(timeline.activities())
                .noneMatch(activity -> "token-usage:602".equals(activity.id()));
    }

    @Test
    void listActivities_filtersByActionType() {
        DataSpecSecurityContext.set(ApiTokenPrincipal.local());
        TestFixture fixture = new TestFixture();
        when(fixture.changeLogRepository.findByProjectId(1L, 100)).thenReturn(List.of(changeLog()));
        when(fixture.snapshotRepository.findRecentByProjectId(1L, 100)).thenReturn(List.of(snapshot()));
        when(fixture.reverseImportBatchRepository.findRecentByProjectId(1L, 100)).thenReturn(List.of());
        when(fixture.sqlCheckRecordRepository.findRecentByProjectId(1L, 100)).thenReturn(List.of());
        when(fixture.aiJobRecordRepository.findRecentByProjectId(1L, 100)).thenReturn(List.of());
        when(fixture.apiTokenRepository.findAllActiveRows()).thenReturn(List.of());

        ProjectActivityTimeline timeline = fixture.service().listActivities(1L, "standard_snapshot", 10);

        assertThat(timeline.activities()).hasSize(1);
        assertThat(timeline.activities().get(0).actionType()).isEqualTo(ProjectActivityServiceImpl.STANDARD_SNAPSHOT);
    }

    @Test
    void listActivities_rejectsUnauthorizedProjectBeforeRepositoryCalls() {
        DataSpecSecurityContext.set(new ApiTokenPrincipal("scoped", "alice", false, Set.of(1L)));
        TestFixture fixture = new TestFixture();

        BizException ex = assertThrows(BizException.class, () -> fixture.service().listActivities(2L, null, 10));

        assertThat(ex.getCode()).isEqualTo(403);
        verify(fixture.changeLogRepository, org.mockito.Mockito.never()).findByProjectId(org.mockito.Mockito.any(), org.mockito.Mockito.anyInt());
    }

    @Test
    void listActivities_hidesTokenUsageForScopedPrincipal() {
        DataSpecSecurityContext.set(new ApiTokenPrincipal("scoped", "alice", false, Set.of(1L)));
        TestFixture fixture = new TestFixture();
        when(fixture.changeLogRepository.findByProjectId(1L, 100)).thenReturn(List.of());
        when(fixture.snapshotRepository.findRecentByProjectId(1L, 100)).thenReturn(List.of());
        when(fixture.reverseImportBatchRepository.findRecentByProjectId(1L, 100)).thenReturn(List.of());
        when(fixture.sqlCheckRecordRepository.findRecentByProjectId(1L, 100)).thenReturn(List.of());
        when(fixture.aiJobRecordRepository.findRecentByProjectId(1L, 100)).thenReturn(List.of());

        ProjectActivityTimeline timeline = fixture.service().listActivities(1L, null, 20);

        assertThat(timeline.activities()).isEmpty();
        verify(fixture.apiTokenRepository, org.mockito.Mockito.never()).findAllActiveRows();
    }

    @Test
    void listActivities_rejectsUnknownActionType() {
        DataSpecSecurityContext.set(ApiTokenPrincipal.local());
        TestFixture fixture = new TestFixture();

        BizException ex = assertThrows(BizException.class, () -> fixture.service().listActivities(1L, "UNKNOWN", 10));

        assertThat(ex.getCode()).isEqualTo(400);
    }

    private StandardChangeLog changeLog() {
        StandardChangeLog log = new StandardChangeLog();
        log.setId(101L);
        log.setProjectId(1L);
        log.setTargetType("field");
        log.setTargetId(9L);
        log.setAction("update");
        log.setOperatorName("alice");
        log.setChangedAt(LocalDateTime.of(2026, 6, 28, 10, 0));
        return log;
    }

    private StandardSnapshot snapshot() {
        StandardSnapshot snapshot = new StandardSnapshot();
        snapshot.setId(201L);
        snapshot.setProjectId(1L);
        snapshot.setVersion("v2026.06.28");
        snapshot.setName("上线前快照");
        snapshot.setSnapshotHash("hash-visible");
        snapshot.setCreatedAt(LocalDateTime.of(2026, 6, 28, 10, 10));
        return snapshot;
    }

    private ReverseImportBatch reverseImportBatch() {
        ReverseImportBatch batch = new ReverseImportBatch();
        batch.setId(251L);
        batch.setProjectId(1L);
        batch.setSourceType("database");
        batch.setDatabaseType("postgresql");
        batch.setDatabaseName("crm");
        batch.setSchemaName("public");
        batch.setImportedCount(3);
        batch.setSkippedCount(1);
        batch.setOperatorName("bob");
        batch.setCreatedAt(LocalDateTime.of(2026, 6, 28, 10, 15));
        return batch;
    }

    private SqlCheckRecord sqlCheckRecord() {
        SqlCheckRecord record = new SqlCheckRecord();
        record.setId(301L);
        record.setProjectId(1L);
        record.setOriginalSql("select * from users");
        record.setFixedSql("CREATE TABLE fixed");
        record.setErrorCount(1);
        record.setWarningCount(1);
        record.setSuggestionCount(0);
        record.setCreatedAt(LocalDateTime.of(2026, 6, 28, 10, 20));
        return record;
    }

    private AiJobRecord aiJobRecord() {
        AiJobRecord record = new AiJobRecord();
        record.setId(401L);
        record.setProjectId(1L);
        record.setJobType("SQL_FIX");
        record.setTitle("修复 SQL");
        record.setInputSummary("修复 orders DDL");
        record.setPromptVersion("fix-sql-prompt@1");
        record.setStatus("FAILED");
        record.setInputPayloadJson("{\"token\":\"ds_secret\"}");
        record.setOutputPayloadJson("{\"tokenHash\":\"hash-secret\"}");
        record.setCreatedAt(LocalDateTime.of(2026, 6, 28, 10, 30));
        return record;
    }

    private ApiToken token(Long id, String projectIds, LocalDateTime lastUsedAt) {
        ApiToken token = new ApiToken();
        token.setId(id);
        token.setName("cli-main");
        token.setOperatorName("carol");
        token.setProjectIds(projectIds);
        token.setTokenHash("hash-secret");
        token.setLastUsedAt(lastUsedAt);
        return token;
    }

    private static class TestFixture {
        private final StandardChangeLogRepository changeLogRepository = mock(StandardChangeLogRepository.class);
        private final StandardSnapshotRepository snapshotRepository = mock(StandardSnapshotRepository.class);
        private final ReverseImportBatchRepository reverseImportBatchRepository = mock(ReverseImportBatchRepository.class);
        private final SqlCheckRecordRepository sqlCheckRecordRepository = mock(SqlCheckRecordRepository.class);
        private final AiJobRecordRepository aiJobRecordRepository = mock(AiJobRecordRepository.class);
        private final ApiTokenRepository apiTokenRepository = mock(ApiTokenRepository.class);

        private ProjectActivityServiceImpl service() {
            return new ProjectActivityServiceImpl(
                    changeLogRepository,
                    snapshotRepository,
                    reverseImportBatchRepository,
                    sqlCheckRecordRepository,
                    aiJobRecordRepository,
                    apiTokenRepository);
        }
    }
}
