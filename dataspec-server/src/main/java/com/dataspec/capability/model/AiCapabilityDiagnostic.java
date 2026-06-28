package com.dataspec.capability.model;

public record AiCapabilityDiagnostic(
        String code,
        String status,
        String message,
        String nextAction
) {
}
