package com.dataspec.common;

import com.dataspec.common.result.ErrorCatalog;
import com.dataspec.common.result.ErrorDetail;
import com.dataspec.common.result.R;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ErrorCatalogTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void failResponseKeepsLegacyFieldsAndAddsDiagnostic() {
        R<Void> response = R.fail(400, "projectId 参数无效: abc");

        assertEquals(400, response.getCode());
        assertEquals("projectId 参数无效: abc", response.getMessage());
        assertNotNull(response.getError());
        assertEquals("PROJECT_ID_INVALID", response.getError().code());
        assertEquals("VALIDATION", response.getError().category());
        assertTrue(response.getError().retryable());
    }

    @Test
    void serializesDiagnosticOnlyForFailedResponse() throws JsonProcessingException {
        String okJson = objectMapper.writeValueAsString(R.ok("ready"));
        String failJson = objectMapper.writeValueAsString(R.fail(400, "projectId 参数无效: abc"));

        assertFalse(okJson.contains("\"error\""));
        assertTrue(failJson.contains("\"error\""));
        assertTrue(failJson.contains("\"PROJECT_ID_INVALID\""));
    }

    @Test
    void failResponseRedactsSensitiveMessageValues() throws JsonProcessingException {
        R<Void> response = R.fail(400, "连接失败 password=secret Authorization: Bearer raw.jwt jdbc:postgresql://localhost/app");
        String json = objectMapper.writeValueAsString(response);

        assertTrue(response.getMessage().contains("[REDACTED]"));
        assertFalse(json.contains("secret"));
        assertFalse(json.contains("raw.jwt"));
        assertFalse(json.contains("jdbc:postgresql://localhost/app"));
    }

    @Test
    void classifiesAuthAndProjectAccessErrors() {
        ErrorDetail token = ErrorCatalog.from(401, "缺少 Authorization Bearer token");
        assertEquals("AUTH_TOKEN_MISSING_OR_INVALID", token.code());
        assertEquals("AUTH", token.category());
        assertTrue(token.retryable());

        ErrorDetail forbidden = ErrorCatalog.from(403, "无权访问项目: 2");
        assertEquals("PROJECT_ACCESS_DENIED", forbidden.code());
        assertEquals("AUTH", forbidden.category());
        assertFalse(forbidden.retryable());
    }

    @Test
    void classifiesDatabaseSqlNotFoundAndInternalErrors() {
        assertEquals(
                "DATABASE_CONNECTION_FAILED",
                ErrorCatalog.from(400, "读取数据库表结构失败: connection refused").code()
        );
        assertEquals(
                "SQL_INPUT_INVALID",
                ErrorCatalog.from(400, "SQL 不能为空").code()
        );
        assertEquals(
                "RESOURCE_NOT_FOUND",
                ErrorCatalog.from(404, "字段不存在: 9").code()
        );
        assertEquals(
                "INTERNAL_ERROR",
                ErrorCatalog.from(500, "服务器内部错误").code()
        );
    }

    @Test
    void classifiesWriteLockConflictAsRetryable() {
        ErrorDetail detail = ErrorCatalog.from(409, "写入操作正在进行，请稍后重试: reverse-import");

        assertEquals("WRITE_OPERATION_IN_PROGRESS", detail.code());
        assertEquals("CONFLICT", detail.category());
        assertTrue(detail.retryable());
    }

    @Test
    void classifiesMissingDryRunEvidenceAsSafetyDiagnostic() {
        ErrorDetail detail = ErrorCatalog.from(400, "缺少 dry-run evidence: operation=project-backup:restore-apply");

        assertEquals("DRY_RUN_REQUIRED", detail.code());
        assertEquals("SAFETY", detail.category());
        assertTrue(detail.retryable());
        assertEquals("project-backup:restore-apply", detail.operation());
        assertEquals(List.of("dryRunToken"), detail.missing());
        assertTrue(Boolean.TRUE.equals(detail.safety().get("requiresDryRun")));
        assertTrue(detail.nextActions().get(0).contains("preview"));
    }
}
