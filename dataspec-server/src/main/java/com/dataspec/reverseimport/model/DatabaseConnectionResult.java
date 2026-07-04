package com.dataspec.reverseimport.model;

/**
 * 数据库连接测试结果。
 */
public record DatabaseConnectionResult(boolean success,
                                       String message,
                                       DatabaseConnectionSecurityDiagnostic security,
                                       DatabaseConnectionHealthDiagnostic health) {

    public DatabaseConnectionResult(boolean success, String message) {
        this(success, message, null, null);
    }

    public DatabaseConnectionResult(boolean success,
                                    String message,
                                    DatabaseConnectionSecurityDiagnostic security) {
        this(success, message, security, null);
    }
}
