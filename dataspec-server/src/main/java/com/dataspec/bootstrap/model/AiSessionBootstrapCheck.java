package com.dataspec.bootstrap.model;

public record AiSessionBootstrapCheck(
        String name,
        String status,
        String message,
        String nextAction
) {
}
