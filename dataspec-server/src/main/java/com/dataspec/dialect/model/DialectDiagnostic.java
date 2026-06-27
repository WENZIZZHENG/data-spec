package com.dataspec.dialect.model;

/**
 * 运行时方言诊断。字段保持扁平结构，方便 AI/CLI 不解析自然语言也能判断风险。
 */
public record DialectDiagnostic(
        String dialect,
        DialectCapability capability,
        DialectSupportLevel level,
        String code,
        String message,
        String nextAction
) {
    public static DialectDiagnostic of(
            String dialect,
            DialectCapability capability,
            DialectSupportLevel level,
            String code,
            String message,
            String nextAction
    ) {
        return new DialectDiagnostic(dialect, capability, level, code, message, nextAction);
    }
}
