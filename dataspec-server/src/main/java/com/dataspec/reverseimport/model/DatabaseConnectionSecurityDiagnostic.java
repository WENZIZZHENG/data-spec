package com.dataspec.reverseimport.model;

import java.util.List;

/**
 * 数据库直连安全诊断。该对象只描述连接账号和 metadata 权限边界，不包含密码、token 或完整连接串。
 */
public record DatabaseConnectionSecurityDiagnostic(
        String databaseType,
        String currentUser,
        Boolean readOnly,
        Boolean writeRisk,
        String riskLevel,
        Integer accessibleSchemaCount,
        Integer accessibleTableCount,
        List<String> warnings,
        List<String> recommendedActions,
        List<String> recommendedSql) {

    public DatabaseConnectionSecurityDiagnostic {
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
        recommendedActions = recommendedActions == null ? List.of() : List.copyOf(recommendedActions);
        recommendedSql = recommendedSql == null ? List.of() : List.copyOf(recommendedSql);
    }
}
