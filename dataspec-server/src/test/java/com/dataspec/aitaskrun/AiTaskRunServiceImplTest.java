package com.dataspec.aitaskrun;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dataspec.aitaskrun.entity.AiTaskRun;
import com.dataspec.aitaskrun.model.AiTaskPartialArtifact;
import com.dataspec.aitaskrun.model.AiTaskRunFinishCommand;
import com.dataspec.aitaskrun.model.AiTaskRunStartCommand;
import com.dataspec.aitaskrun.model.AiTaskStepStatus;
import com.dataspec.aitaskrun.repository.AiTaskRunRepository;
import com.dataspec.aitaskrun.service.impl.AiTaskRunServiceImpl;
import com.dataspec.common.exception.BizException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiTaskRunServiceImplTest {

    @Test
    void startAndPartialFail_sanitizesMetadataAndStoresResumeState() {
        AiTaskRunRepository repository = mock(AiTaskRunRepository.class);
        when(repository.insert(any(AiTaskRun.class))).thenAnswer(invocation -> {
            AiTaskRun run = invocation.getArgument(0);
            run.setId(7L);
            run.setCreatedAt(LocalDateTime.of(2026, 7, 4, 10, 0));
            return 1;
        });
        AiTaskRunServiceImpl service = new AiTaskRunServiceImpl(repository, new ObjectMapper().findAndRegisterModules());

        AiTaskRun run = service.start(new AiTaskRunStartCommand(
                1L,
                "SQL_LINT",
                "AI_BATCH",
                "hash-1",
                "token=plain-secret",
                List.of(new AiTaskStepStatus("lint-items", "RUNNING", "Bearer abc", null)),
                List.of(),
                Map.of("jdbcUrl", "jdbc:postgresql://localhost/db", "note", "password=p@ss"),
                LocalDateTime.of(2026, 7, 11, 10, 0)
        ));
        service.partialFail(run, new AiTaskRunFinishCommand(
                42L,
                true,
                "lint-items",
                "node tools/dataspec-cli.mjs lint-files bad.sql --idempotency-key token=plain-secret",
                "password=p@ss 后重试",
                List.of(new AiTaskStepStatus("lint-items", "PARTIAL_FAILED", "jdbc:postgresql://localhost/db", "ai-batch:42")),
                List.of(new AiTaskPartialArtifact("sql", "bad.sql", "bad.sql", "Bearer abc")),
                Map.of("connectionString", "jdbc:mysql://localhost/demo"),
                LocalDateTime.of(2026, 7, 11, 10, 0)
        ));

        ArgumentCaptor<AiTaskRun> insertCaptor = ArgumentCaptor.forClass(AiTaskRun.class);
        verify(repository).insert(insertCaptor.capture());
        assertThat(insertCaptor.getValue().getIdempotencyKey()).contains("[REDACTED]");
        assertThat(insertCaptor.getValue().getStepStatusJson()).doesNotContain("Bearer abc");
        assertThat(insertCaptor.getValue().getMetadataJson()).doesNotContain("jdbc:postgresql://localhost/db", "p@ss");

        ArgumentCaptor<AiTaskRun> updateCaptor = ArgumentCaptor.forClass(AiTaskRun.class);
        verify(repository).update(updateCaptor.capture());
        AiTaskRun updated = updateCaptor.getValue();
        assertThat(updated.getStatus()).isEqualTo("PARTIAL_FAILED");
        assertThat(updated.getSourceId()).isEqualTo(42L);
        assertThat(updated.getRetryable()).isTrue();
        assertThat(updated.getFailedStep()).isEqualTo("lint-items");
        assertThat(updated.getResumeCommand()).contains("[REDACTED]");
        assertThat(updated.getNextAction()).doesNotContain("p@ss");
        assertThat(updated.getPartialArtifactsJson()).doesNotContain("Bearer abc");
        assertThat(updated.getMetadataJson()).doesNotContain("jdbc:mysql://localhost/demo");
    }

    @Test
    void listRecentFailuresAndDetail_areProjectScopedAndParseJson() {
        AiTaskRunRepository repository = mock(AiTaskRunRepository.class);
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        AiTaskRun run = taskRun(7L, 1L, "PARTIAL_FAILED");
        run.setStepStatusJson("[{\"step\":\"lint-items\",\"status\":\"PARTIAL_FAILED\",\"message\":\"失败\",\"artifactRef\":\"ai-batch:42\"}]");
        run.setPartialArtifactsJson("[{\"type\":\"ai-batch\",\"name\":\"package\",\"ref\":\"ai-batch:42\",\"summary\":\"partial\"}]");
        run.setMetadataJson("{\"source\":\"cli\"}");
        when(repository.findById(7L)).thenReturn(Optional.of(run));
        when(repository.findRecentFailures(1L, 5)).thenReturn(List.of(run));
        Page<AiTaskRun> page = new Page<>(1, 10, 1);
        page.setRecords(List.of(run));
        when(repository.findByProjectId(1L, "PARTIAL_FAILED", "SQL_LINT", 1, 10)).thenReturn(page);
        AiTaskRunServiceImpl service = new AiTaskRunServiceImpl(repository, objectMapper);

        assertThat(service.recentFailures(1L, 5)).hasSize(1);
        assertThat(service.list(1L, "PARTIAL_FAILED", "SQL_LINT", 1, 10).getRecords()).hasSize(1);
        var detail = service.detail(1L, 7L);

        assertThat(detail.stepStatus()).extracting("step").containsExactly("lint-items");
        assertThat(detail.partialArtifacts()).extracting("ref").containsExactly("ai-batch:42");
        assertThat(detail.metadata()).containsEntry("source", "cli");
        assertThrows(BizException.class, () -> service.detail(2L, 7L));
    }

    private AiTaskRun taskRun(Long id, Long projectId, String status) {
        AiTaskRun run = new AiTaskRun();
        run.setId(id);
        run.setProjectId(projectId);
        run.setTaskType("SQL_LINT");
        run.setSourceType("AI_BATCH");
        run.setSourceId(42L);
        run.setStatus(status);
        run.setRetryable(true);
        run.setFailedStep("lint-items");
        run.setResumeCommand("node tools/dataspec-cli.mjs task show 7 --project 1 --format json");
        run.setNextAction("修正失败 SQL 后重试");
        run.setStartedAt(LocalDateTime.of(2026, 7, 4, 10, 0));
        run.setCreatedAt(LocalDateTime.of(2026, 7, 4, 10, 0));
        return run;
    }
}
