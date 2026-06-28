package com.dataspec.idempotency;

import com.dataspec.common.exception.BizException;
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
    void execute_rejectsConcurrentOperationWithRetryableConflict() throws Exception {
        WriteGuardService service = new WriteGuardService();
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        var executor = Executors.newSingleThreadExecutor();
        try {
            var running = executor.submit(() -> service.execute(1L, "reverse-import:apply", null, () -> {
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
                    () -> service.execute(1L, "reverse-import:apply", null, () -> "second"));

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
                service.execute(1L, "reverse-import:apply", null, () -> "created"));
        var executor = Executors.newSingleThreadExecutor();
        try {
            BizException error = executor.submit(() -> assertThrows(BizException.class,
                    () -> service.execute(1L, "reverse-import:apply", null, () -> "second"))).get(2, TimeUnit.SECONDS);

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
