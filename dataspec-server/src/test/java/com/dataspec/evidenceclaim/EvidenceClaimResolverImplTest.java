package com.dataspec.evidenceclaim;

import com.dataspec.aibatch.entity.AiBatchRun;
import com.dataspec.aibatch.repository.AiBatchRunRepository;
import com.dataspec.aireplay.entity.AiJobRecord;
import com.dataspec.aireplay.repository.AiJobRecordRepository;
import com.dataspec.aitaskrun.entity.AiTaskRun;
import com.dataspec.aitaskrun.repository.AiTaskRunRepository;
import com.dataspec.evidence.model.EvidenceSourceType;
import com.dataspec.evidenceclaim.model.EvidenceClaimResolutionStatus;
import com.dataspec.evidenceclaim.service.impl.EvidenceClaimResolverImpl;
import com.dataspec.lint.entity.SqlCheckRecord;
import com.dataspec.lint.repository.SqlCheckRecordRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EvidenceClaimResolverImplTest {

    @Test
    void resolveDistinguishesVerifiedMissingCrossProjectAndUnverifiableClaims() {
        SqlCheckRecordRepository sqlChecks = mock(SqlCheckRecordRepository.class);
        AiJobRecordRepository aiJobs = mock(AiJobRecordRepository.class);
        AiBatchRunRepository batchRuns = mock(AiBatchRunRepository.class);
        AiTaskRunRepository taskRuns = mock(AiTaskRunRepository.class);
        when(sqlChecks.findById(10L)).thenReturn(Optional.of(sqlCheck(10L, 1L)));
        when(sqlChecks.findById(11L)).thenReturn(Optional.of(sqlCheck(11L, null)));
        when(aiJobs.findById(20L)).thenReturn(Optional.empty());
        when(batchRuns.findById(30L)).thenReturn(Optional.of(batchRun(30L, 2L)));
        when(taskRuns.findById(40L)).thenReturn(Optional.of(taskRun(40L, 1L)));
        EvidenceClaimResolverImpl resolver = new EvidenceClaimResolverImpl(sqlChecks, aiJobs, batchRuns, taskRuns);

        var verified = resolver.resolve(1L, "dataspec://evidence/sql-check/10");
        assertEquals(EvidenceClaimResolutionStatus.VERIFIED, verified.status());
        assertEquals(EvidenceSourceType.SQL_CHECK, verified.sourceType());
        assertEquals("dataspec://evidence/sql-check/10", verified.canonicalRef());

        var anonymous = resolver.resolve(1L, "dataspec://evidence/sql-check/11");
        assertEquals(EvidenceClaimResolutionStatus.UNVERIFIABLE, anonymous.status());

        var missing = resolver.resolve(1L, "dataspec://evidence/ai-job/20");
        assertEquals(EvidenceClaimResolutionStatus.MISSING, missing.status());
        assertNull(missing.canonicalRef());

        var crossProject = resolver.resolve(1L, "dataspec://evidence/ai-batch-run/30");
        assertEquals(EvidenceClaimResolutionStatus.CROSS_PROJECT, crossProject.status());
        assertNull(crossProject.canonicalRef());
        assertNull(crossProject.sourceProjectId());

        var task = resolver.resolve(1L, "dataspec://evidence/ai-task-run/40");
        assertEquals(EvidenceClaimResolutionStatus.VERIFIED, task.status());
        assertEquals(EvidenceSourceType.AI_TASK_RUN, task.sourceType());

        var unsupported = resolver.resolve(1L, "dataspec://evidence/coverage-report/9");
        assertEquals(EvidenceClaimResolutionStatus.UNVERIFIABLE, unsupported.status());
        assertNull(unsupported.canonicalRef());

        var secretLike = resolver.resolve(1L, "dataspec://evidence/sql-check/10?token=raw-secret-123");
        assertEquals(EvidenceClaimResolutionStatus.UNVERIFIABLE, secretLike.status());
        assertThat(secretLike.inputRef()).doesNotContain("raw-secret-123");
    }

    private SqlCheckRecord sqlCheck(Long id, Long projectId) {
        SqlCheckRecord record = new SqlCheckRecord();
        record.setId(id);
        record.setProjectId(projectId);
        return record;
    }

    private AiBatchRun batchRun(Long id, Long projectId) {
        AiBatchRun run = new AiBatchRun();
        run.setId(id);
        run.setProjectId(projectId);
        return run;
    }

    private AiTaskRun taskRun(Long id, Long projectId) {
        AiTaskRun run = new AiTaskRun();
        run.setId(id);
        run.setProjectId(projectId);
        return run;
    }
}
