package com.dataspec.idempotency;

import com.dataspec.common.exception.BizException;
import com.dataspec.common.result.R;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WriteGuardServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void execute_reusesSuccessfulResultForSameProjectOperationAndKey() {
        WriteGuardService service = new WriteGuardService();
        AtomicInteger executions = new AtomicInteger();

        String first = service.execute(1L, "snapshot:create", "retry-1", () -> {
            executions.incrementAndGet();
            return "created";
        });
        String second = service.execute(1L, "snapshot:create", "retry-1", () -> {
            executions.incrementAndGet();
            return "duplicated";
        });

        assertThat(first).isEqualTo("created");
        assertThat(second).isEqualTo("created");
        assertThat(executions).hasValue(1);
    }

    @Test
    void execute_scopesSameKeyByOperation() {
        WriteGuardService service = new WriteGuardService();

        String snapshot = service.execute(1L, "snapshot:create", "same-key", () -> "snapshot");
        String restore = service.execute(1L, "restore:apply", "same-key", () -> "restore");

        assertThat(snapshot).isEqualTo("snapshot");
        assertThat(restore).isEqualTo("restore");
    }

    @Test
    void execute_rejectsMissingIdempotencyKeyForHighRiskApplyOperation() {
        WriteGuardService service = new WriteGuardService();

        BizException error = assertThrows(BizException.class,
                () -> service.execute(1L, "project-backup:restore-apply", null, () -> "unsafe"));
        JsonNode response = objectMapper.valueToTree(R.fail(error.getCode(), error.getMessage()));
        String serialized = response.toString();

        assertThat(error.getCode()).isEqualTo(400);
        assertThat(error.getMessage()).contains("Idempotency-Key");
        assertThat(response.path("error").path("code").asText()).isEqualTo("IDEMPOTENCY_KEY_REQUIRED");
        assertThat(response.path("error").path("category").asText()).isEqualTo("SAFETY");
        assertThat(response.path("error").path("retryable").asBoolean()).isTrue();
        assertThat(response.path("error").path("missing").toString()).contains("Idempotency-Key");
        assertThat(response.path("error").path("operation").asText()).isEqualTo("project-backup:restore-apply");
        assertThat(response.path("error").path("safety").path("requiresIdempotencyKey").asBoolean()).isTrue();
        assertThat(response.path("error").path("nextActions").toString()).contains("idempotency");
        assertThat(serialized).doesNotContain("password=raw")
                .doesNotContain("Authorization")
                .doesNotContain("jdbc:postgresql://");
    }

    @Test
    void execute_rejectsConcurrentOperationWithRetryableConflict() throws Exception {
        WriteGuardService service = new WriteGuardService();
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        var executor = Executors.newSingleThreadExecutor();
        try {
            var running = executor.submit(() -> service.execute(1L, "low-risk:write", null, () -> {
                started.countDown();
                try {
                    release.await(2, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return "done";
            }));
            assertThat(started.await(2, TimeUnit.SECONDS)).isTrue();

            BizException error = assertThrows(BizException.class,
                    () -> service.execute(1L, "low-risk:write", null, () -> "second"));

            assertThat(error.getCode()).isEqualTo(409);
            assertThat(error.getMessage()).contains("写入操作正在进行");
            release.countDown();
            assertThat(running.get(2, TimeUnit.SECONDS)).isEqualTo("done");
        } finally {
            release.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void execute_keepsTransactionalOperationLockedUntilCompletionWithoutKey() throws Exception {
        WriteGuardService service = new WriteGuardService();

        List<TransactionSynchronization> synchronizations = runInsideTransaction(() ->
                service.execute(1L, "low-risk:write", null, () -> "created"));
        var executor = Executors.newSingleThreadExecutor();
        try {
            BizException error = executor.submit(() -> assertThrows(BizException.class,
                    () -> service.execute(1L, "low-risk:write", null, () -> "second"))).get(2, TimeUnit.SECONDS);

            assertThat(error.getCode()).isEqualTo(409);
            assertThat(error.getMessage()).contains("写入操作正在进行");
        } finally {
            completeTransaction(synchronizations, true);
            executor.shutdownNow();
        }
    }

    @Test
    void execute_cachesTransactionalResultAfterCommit() {
        WriteGuardService service = new WriteGuardService();
        AtomicInteger executions = new AtomicInteger();

        List<TransactionSynchronization> synchronizations = runInsideTransaction(() ->
                service.execute(1L, "snapshot:create", "retry-after-commit", () -> {
                    executions.incrementAndGet();
                    return "created";
                }));
        completeTransaction(synchronizations, true);

        String second = service.execute(1L, "snapshot:create", "retry-after-commit", () -> {
            executions.incrementAndGet();
            return "duplicated";
        });

        assertThat(second).isEqualTo("created");
        assertThat(executions).hasValue(1);
    }

    @Test
    void execute_doesNotCacheTransactionalResultAfterRollback() {
        WriteGuardService service = new WriteGuardService();
        AtomicInteger executions = new AtomicInteger();

        List<TransactionSynchronization> synchronizations = runInsideTransaction(() ->
                service.execute(1L, "snapshot:create", "retry-after-rollback", () -> {
                    executions.incrementAndGet();
                    return "rolled-back";
                }));
        completeTransaction(synchronizations, false);

        String second = service.execute(1L, "snapshot:create", "retry-after-rollback", () -> {
            executions.incrementAndGet();
            return "created-again";
        });

        assertThat(second).isEqualTo("created-again");
        assertThat(executions).hasValue(2);
    }

    private List<TransactionSynchronization> runInsideTransaction(Runnable action) {
        TransactionSynchronizationManager.initSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(true);
        try {
            action.run();
            return TransactionSynchronizationManager.getSynchronizations();
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
            TransactionSynchronizationManager.setActualTransactionActive(false);
        }
    }

    private void completeTransaction(List<TransactionSynchronization> synchronizations, boolean committed) {
        if (committed) {
            synchronizations.forEach(TransactionSynchronization::afterCommit);
        }
        int status = committed
                ? TransactionSynchronization.STATUS_COMMITTED
                : TransactionSynchronization.STATUS_ROLLED_BACK;
        synchronizations.forEach(synchronization -> synchronization.afterCompletion(status));
    }
}
