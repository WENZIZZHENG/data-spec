package com.dataspec.idempotency;

import com.dataspec.common.exception.BizException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/**
 * 单机轻量写入保护。
 *
 * <p>第一版面向个人/小团队的单后端进程：使用内存缓存复用同一幂等 key 的成功结果，并用
 * project + operation 维度的 try-lock 阻止高风险写入并发交错。它不是分布式锁，也不承诺服务重启后的历史幂等。</p>
 */
@Service
public class WriteGuardService {

    private static final int MAX_KEY_LENGTH = 128;
    private static final int MAX_COMPLETED_ENTRIES = 1_000;
    private static final Duration COMPLETED_ENTRY_TTL = Duration.ofHours(6);

    private final Map<String, CompletedEntry> completedEntries = new ConcurrentHashMap<>();
    private final Map<String, ReentrantLock> operationLocks = new ConcurrentHashMap<>();

    public <T> T execute(Long projectId, String operation, String idempotencyKey, Supplier<T> action) {
        if (projectId == null) {
            throw new BizException(400, "项目 ID 不能为空");
        }
        if (operation == null || operation.isBlank()) {
            throw new BizException(400, "写入操作名称不能为空");
        }
        String operationScope = projectId + ":" + operation.trim();
        String normalizedKey = normalizeKey(idempotencyKey);
        String completedKey = normalizedKey == null ? null : operationScope + ":" + normalizedKey;
        if (completedKey != null) {
            CompletedEntry completed = completedEntries.get(completedKey);
            if (completed != null) {
                return completed.typedValue();
            }
        }

        ReentrantLock lock = operationLocks.computeIfAbsent(operationScope, ignored -> new ReentrantLock());
        if (!lock.tryLock()) {
            throw new BizException(409, "写入操作正在进行，请稍后重试: " + operation.trim());
        }
        boolean unlockInFinally = true;
        try {
            if (completedKey != null) {
                CompletedEntry completed = completedEntries.get(completedKey);
                if (completed != null) {
                    return completed.typedValue();
                }
            }
            T result = action.get();
            unlockInFinally = finishAfterCommitOrImmediately(completedKey, result, lock);
            return result;
        } finally {
            if (unlockInFinally) {
                lock.unlock();
            }
        }
    }

    private String normalizeKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return null;
        }
        String key = idempotencyKey.trim();
        if (key.length() > MAX_KEY_LENGTH) {
            throw new BizException(400, "Idempotency-Key 不能超过 " + MAX_KEY_LENGTH + " 个字符");
        }
        return key;
    }

    private <T> boolean finishAfterCommitOrImmediately(String completedKey, T result, ReentrantLock lock) {
        if (TransactionSynchronizationManager.isSynchronizationActive()
                && TransactionSynchronizationManager.isActualTransactionActive()) {
            // 事务写入只有提交成功后才能被视为幂等成功；锁也要覆盖到提交完成，避免提交前窗口内重复写入。
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    if (completedKey != null) {
                        storeCompletedEntry(completedKey, result);
                    }
                }

                @Override
                public void afterCompletion(int status) {
                    lock.unlock();
                }
            });
            return false;
        }
        if (completedKey != null) {
            storeCompletedEntry(completedKey, result);
        }
        return true;
    }

    private void storeCompletedEntry(String completedKey, Object result) {
        completedEntries.put(completedKey, new CompletedEntry(result, Instant.now()));
        evictCompletedEntries();
    }

    private void evictCompletedEntries() {
        Instant expiredBefore = Instant.now().minus(COMPLETED_ENTRY_TTL);
        completedEntries.entrySet().removeIf(entry -> entry.getValue().createdAt().isBefore(expiredBefore));
        if (completedEntries.size() <= MAX_COMPLETED_ENTRIES) {
            return;
        }
        int removeCount = completedEntries.size() - MAX_COMPLETED_ENTRIES;
        completedEntries.entrySet().stream()
                .sorted(Comparator.comparing(entry -> entry.getValue().createdAt()))
                .limit(removeCount)
                .map(Map.Entry::getKey)
                .toList()
                .forEach(completedEntries::remove);
    }

    private record CompletedEntry(Object value, Instant createdAt) {
        @SuppressWarnings("unchecked")
        private <T> T typedValue() {
            return (T) value;
        }
    }
}
