package com.dataspec.reverseimport.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * 数据库连接健康诊断。失败场景也会返回该对象，供前端和 AI 判断下一步动作。
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record DatabaseConnectionHealthDiagnostic(
        String connectionStatus,
        Long latencyMs,
        String databaseProduct,
        String version,
        String dialect,
        String failureCategory,
        Boolean retryable,
        String message,
        DatabaseDialectCapability capability,
        String readonlyCheck,
        List<String> requiredPrivileges,
        List<String> warnings,
        List<String> nextActions) {

    public DatabaseConnectionHealthDiagnostic {
        requiredPrivileges = requiredPrivileges == null ? List.of() : List.copyOf(requiredPrivileges);
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
        nextActions = nextActions == null ? List.of() : List.copyOf(nextActions);
    }
}
