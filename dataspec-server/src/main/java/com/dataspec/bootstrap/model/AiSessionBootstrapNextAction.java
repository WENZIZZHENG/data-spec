package com.dataspec.bootstrap.model;

public record AiSessionBootstrapNextAction(
        String code,
        String severity,
        String message,
        String command,
        String docsRef,
        boolean retryable
) {
}
