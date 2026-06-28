package com.dataspec.aibatch;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dataspec.aibatch.entity.AiBatchRun;
import com.dataspec.aibatch.model.AiBatchDeliveryPackage;
import com.dataspec.aibatch.model.AiBatchRunDetail;
import com.dataspec.aibatch.model.AiBatchSqlLintItemReq;
import com.dataspec.aibatch.model.AiBatchSqlLintReq;
import com.dataspec.aibatch.repository.AiBatchRunRepository;
import com.dataspec.aibatch.service.impl.AiBatchServiceImpl;
import com.dataspec.common.exception.BizException;
import com.dataspec.lint.engine.SqlLintService;
import com.dataspec.lint.model.LintIssue;
import com.dataspec.lint.model.LintResult;
import com.dataspec.lint.model.Severity;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiBatchServiceImplTest {

    @Test
    void createSqlLintBatch_aggregatesItemResultsAndSanitizesStoredPayload() {
        SqlLintService sqlLintService = mock(SqlLintService.class);
        AiBatchRunRepository repository = mock(AiBatchRunRepository.class);
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        ArgumentCaptor<AiBatchRun> insertCaptor = ArgumentCaptor.forClass(AiBatchRun.class);
        ArgumentCaptor<AiBatchRun> updateCaptor = ArgumentCaptor.forClass(AiBatchRun.class);
        when(repository.insert(insertCaptor.capture())).thenAnswer(invocation -> {
            AiBatchRun run = invocation.getArgument(0);
            run.setId(42L);
            run.setCreatedAt(LocalDateTime.of(2026, 6, 28, 10, 30));
            return 1;
        });
        when(repository.update(updateCaptor.capture())).thenReturn(1);
        when(sqlLintService.lint("CREATE TABLE UserOrder(id bigint);", 1L)).thenReturn(errorResultWithFixedSql());
        when(sqlLintService.lint("password='quoted-secret' token=ds_secret jdbc:postgresql://localhost/db", 1L))
                .thenThrow(new IllegalArgumentException("Bearer abc password='quoted-secret' token=ds_secret jdbc:postgresql://localhost/db"));
        AiBatchServiceImpl service = new AiBatchServiceImpl(repository, sqlLintService, objectMapper);

        AiBatchDeliveryPackage pkg = service.createSqlLintBatch(new AiBatchSqlLintReq(
                1L,
                "Bearer abc jdbc:postgresql://localhost/db",
                List.of(
                        new AiBatchSqlLintItemReq("orders.sql", "sql/orders.sql", "CREATE TABLE UserOrder(id bigint);"),
                        new AiBatchSqlLintItemReq("bad.sql", "sql/bad.sql", "password='quoted-secret' token=ds_secret jdbc:postgresql://localhost/db")
                )
        ));

        assertThat(pkg.batchId()).isEqualTo("server-42");
        assertThat(pkg.status()).isEqualTo("PARTIAL_FAILED");
        assertThat(pkg.summary().totalItems()).isEqualTo(2);
        assertThat(pkg.summary().successItems()).isEqualTo(1);
        assertThat(pkg.summary().failedItems()).isEqualTo(1);
        assertThat(pkg.issueSummary().errorCount()).isEqualTo(1);
        assertThat(pkg.fixedSqlSummary().availableCount()).isEqualTo(1);
        assertThat(pkg.items().get(0).fixedSql()).contains("user_order");
        assertThat(pkg.items().get(1).errorMessage()).doesNotContain("quoted-secret", "ds_secret", "Bearer abc", "jdbc:postgresql://localhost/db");

        AiBatchRun inserted = insertCaptor.getValue();
        assertThat(inserted.getBatchType()).isEqualTo("SQL_LINT");
        assertThat(inserted.getSource()).doesNotContain("Bearer abc", "jdbc:postgresql://localhost/db");
        assertThat(inserted.getOperatorName()).isEqualTo("local");

        AiBatchRun updated = updateCaptor.getValue();
        assertThat(updated.getSummaryJson()).contains("\"totalItems\":2");
        assertThat(updated.getPayloadJson()).contains("\"batchId\":\"server-42\"");
        assertThat(updated.getPayloadJson()).doesNotContain("quoted-secret", "ds_secret", "Bearer abc", "jdbc:postgresql://localhost/db");
    }

    @Test
    void createSqlLintBatch_reusesDeliveryPackageForSameIdempotencyKey() {
        SqlLintService sqlLintService = mock(SqlLintService.class);
        AiBatchRunRepository repository = mock(AiBatchRunRepository.class);
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        when(repository.insert(any(AiBatchRun.class))).thenAnswer(invocation -> {
            AiBatchRun run = invocation.getArgument(0);
            run.setId(42L);
            run.setCreatedAt(LocalDateTime.of(2026, 6, 28, 10, 30));
            return 1;
        });
        when(repository.update(any(AiBatchRun.class))).thenReturn(1);
        when(sqlLintService.lint("CREATE TABLE users(id bigint);", 1L)).thenReturn(LintResult.of(List.of(), List.of()));
        AiBatchServiceImpl service = new AiBatchServiceImpl(repository, sqlLintService, objectMapper);
        AiBatchSqlLintReq req = new AiBatchSqlLintReq(
                1L,
                "cli",
                List.of(new AiBatchSqlLintItemReq("users.sql", "sql/users.sql", "CREATE TABLE users(id bigint);"))
        );

        AiBatchDeliveryPackage first = service.createSqlLintBatch(req, "retry-batch-1");
        AiBatchDeliveryPackage second = service.createSqlLintBatch(req, "retry-batch-1");

        assertThat(second).isSameAs(first);
        verify(repository, times(1)).insert(any(AiBatchRun.class));
        verify(repository, times(1)).update(any(AiBatchRun.class));
        verify(sqlLintService, times(1)).lint("CREATE TABLE users(id bigint);", 1L);
    }

    @Test
    void createSqlLintBatch_rejectsMissingProjectOrItems() {
        AiBatchServiceImpl service = new AiBatchServiceImpl(
                mock(AiBatchRunRepository.class),
                mock(SqlLintService.class),
                new ObjectMapper().findAndRegisterModules()
        );

        assertThrows(BizException.class, () -> service.createSqlLintBatch(new AiBatchSqlLintReq(null, "frontend", List.of())));
        assertThrows(BizException.class, () -> service.createSqlLintBatch(new AiBatchSqlLintReq(1L, "frontend", List.of())));
    }

    @Test
    void listByProject_delegatesToRepository() {
        AiBatchRunRepository repository = mock(AiBatchRunRepository.class);
        Page<AiBatchRun> page = new Page<>(1, 10);
        when(repository.findByProjectId(1L, 1, 10)).thenReturn(page);
        AiBatchServiceImpl service = new AiBatchServiceImpl(repository, mock(SqlLintService.class), new ObjectMapper().findAndRegisterModules());

        IPage<AiBatchRun> result = service.listByProject(1L, 1, 10);

        assertThat(result).isSameAs(page);
    }

    @Test
    void getDetail_parsesStoredPackagePayload() {
        AiBatchRun run = new AiBatchRun();
        run.setId(42L);
        run.setProjectId(1L);
        run.setBatchType("SQL_LINT");
        run.setSource("frontend");
        run.setStatus("SUCCESS");
        run.setSummaryJson("{\"totalItems\":1}");
        run.setPayloadJson("""
                {"packageVersion":"ai-batch-delivery@1","batchId":"server-42","projectId":1,"batchType":"SQL_LINT","source":"frontend","status":"SUCCESS","summary":{"totalItems":1,"successItems":1,"failedItems":0,"errorCount":0,"warningCount":0,"suggestionCount":0,"fixedSqlCount":0},"items":[],"issueSummary":{"errorCount":0,"warningCount":0,"suggestionCount":0,"byRule":[]},"fixedSqlSummary":{"availableCount":0,"changedCount":0},"unmanagedHints":[],"evidence":[],"nextActions":[],"createdAt":"2026-06-28T10:30:00"}
                """);
        AiBatchRunRepository repository = mock(AiBatchRunRepository.class);
        when(repository.findById(42L)).thenReturn(Optional.of(run));
        AiBatchServiceImpl service = new AiBatchServiceImpl(repository, mock(SqlLintService.class), new ObjectMapper().findAndRegisterModules());

        AiBatchRunDetail detail = service.getDetail(42L);

        assertThat(detail.run().getId()).isEqualTo(42L);
        assertThat(detail.deliveryPackage().batchId()).isEqualTo("server-42");
    }

    private LintResult errorResultWithFixedSql() {
        LintIssue issue = LintIssue.builder()
                .severity(Severity.ERROR)
                .ruleCode("table_naming_snake_case")
                .ruleName("表名 snake_case")
                .message("表名 UserOrder 不符合 snake_case")
                .tableName("UserOrder")
                .suggestion("改为 user_order")
                .build();
        LintResult result = LintResult.of(List.of(), List.of(issue));
        result.setFixedSql("CREATE TABLE user_order(id bigint);");
        result.setFixedSqlDiff("--- before\n+++ after\n");
        return result;
    }
}
