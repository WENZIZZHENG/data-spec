package com.dataspec.common;

import com.dataspec.common.sanitize.SensitiveDataSanitizer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SensitiveDataSanitizerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void redactsCommonSecretTextPatterns() {
        String raw = "Authorization: Bearer abc.def Authorization: Basic raw-basic password=s3cr3t token=ds_raw jdbc:postgresql://localhost:5432/app dsn=postgres://user:pass@host/db mysql://user:pass@host/db https://user:pass@example.com/path";
        String sanitized = SensitiveDataSanitizer.redactText(raw);

        assertTrue(sanitized.contains("[REDACTED]"));
        assertTrue(sanitized.contains("Authorization: Bearer [REDACTED]"));
        assertTrue(sanitized.contains("Authorization: Basic [REDACTED]"));
        assertFalse(sanitized.contains("Authorization: [REDACTED] [REDACTED]"));
        assertFalse(sanitized.contains("abc.def"));
        assertFalse(sanitized.contains("raw-basic"));
        assertFalse(sanitized.contains("s3cr3t"));
        assertFalse(sanitized.contains("ds_raw"));
        assertFalse(sanitized.contains("jdbc:postgresql://localhost"));
        assertFalse(sanitized.contains("user:pass@host"));
        assertFalse(sanitized.contains("user:pass@example.com"));
        assertFalse(sanitized.contains("dsn=postgres://"));
    }

    @Test
    void redactsExplicitSecretAndLimitsLength() {
        String sanitized = SensitiveDataSanitizer.redactText(
                "driver said login failed for raw-password-value and connectionString=server",
                45,
                "raw-password-value"
        );

        assertFalse(sanitized.contains("raw-password-value"));
        assertTrue(sanitized.length() <= 48);
    }

    @Test
    void sanitizesNestedPayloadByKeyAndValue() {
        Object sanitized = SensitiveDataSanitizer.sanitizeValue(Map.of(
                "projectId", 7,
                "password", "secret",
                "items", List.of(Map.of("message", "Bearer abc token=ds_raw"))
        ));

        String text = sanitized.toString();
        assertTrue(text.contains("password=[REDACTED]"));
        assertFalse(text.contains("secret"));
        assertFalse(text.contains("Bearer abc"));
        assertFalse(text.contains("ds_raw"));
    }

    @Test
    void detectsSensitiveJsonKeysAndValues() throws Exception {
        JsonNode keyNode = objectMapper.readTree("{\"apiToken\":\"ds_raw\"}");
        JsonNode valueNode = objectMapper.readTree("{\"message\":\"jdbc:mysql://localhost/app?password=secret\"}");
        JsonNode safeNode = objectMapper.readTree("{\"databaseName\":\"app\",\"host\":\"localhost\"}");

        assertTrue(SensitiveDataSanitizer.containsSensitiveKeyOrValue(keyNode));
        assertTrue(SensitiveDataSanitizer.containsSensitiveKeyOrValue(valueNode));
        assertFalse(SensitiveDataSanitizer.containsSensitiveKeyOrValue(safeNode));
    }

    @Test
    void classifiesSensitiveKeysConservatively() {
        assertTrue(SensitiveDataSanitizer.isSensitiveKey("tokenHash"));
        assertTrue(SensitiveDataSanitizer.isSensitiveKey("jdbc_url"));
        assertTrue(SensitiveDataSanitizer.isSensitiveKey("dbPassword"));
        assertTrue(SensitiveDataSanitizer.isSensitiveKey("githubApiKey"));
        assertTrue(SensitiveDataSanitizer.isSensitiveKey("sourceTokenHash"));
        assertTrue(SensitiveDataSanitizer.isSensitiveKey("sourceDsn"));
        assertFalse(SensitiveDataSanitizer.isSensitiveKey("databaseName"));
        assertEquals("password=[REDACTED]", SensitiveDataSanitizer.sanitizeValue("password=secret"));
    }
}
