package com.dataspec.common.perf;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Supplier;

/**
 * 轻量性能探针，用于个人/小团队场景下发现明显慢操作。
 */
public final class PerformanceProbe {

    private static final Logger log = LoggerFactory.getLogger(PerformanceProbe.class);

    private PerformanceProbe() {
    }

    public static <T> T measure(String operation, long thresholdMs, String hint, Supplier<T> supplier) {
        long startNanos = System.nanoTime();
        try {
            return supplier.get();
        } finally {
            logSample(sample(operation, elapsedMs(startNanos), thresholdMs, hint));
        }
    }

    public static void measure(String operation, long thresholdMs, String hint, Runnable runnable) {
        measure(operation, thresholdMs, hint, () -> {
            runnable.run();
            return null;
        });
    }

    public static PerformanceSample sample(String operation, long durationMs, long thresholdMs, String hint) {
        return new PerformanceSample(operation, durationMs, thresholdMs, hint);
    }

    private static long elapsedMs(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }

    private static void logSample(PerformanceSample sample) {
        if (sample.slow()) {
            log.warn(
                    "DataSpec slow operation: operation={}, durationMs={}, thresholdMs={}, hint={}",
                    sample.operation(),
                    sample.durationMs(),
                    sample.thresholdMs(),
                    sample.hint());
            return;
        }
        log.debug(
                "DataSpec operation duration: operation={}, durationMs={}, thresholdMs={}",
                sample.operation(),
                sample.durationMs(),
                sample.thresholdMs());
    }

    public record PerformanceSample(
            String operation,
            long durationMs,
            long thresholdMs,
            String hint
    ) {
        public boolean slow() {
            return thresholdMs >= 0 && durationMs > thresholdMs;
        }
    }
}
