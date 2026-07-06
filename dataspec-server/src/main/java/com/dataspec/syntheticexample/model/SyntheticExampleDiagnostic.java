package com.dataspec.syntheticexample.model;

/**
 * 合成样例诊断项，描述预期规则命中、生成降级或参数问题。
 *
 * @param id 稳定诊断标识，用于 bad SQL case 和测试断言引用。
 * @param severity 诊断级别，例如 INFO、WARN、ERROR。
 * @param message 面向用户和 AI 的脱敏说明。
 */
public record SyntheticExampleDiagnostic(
        String id,
        String severity,
        String message
) {
    /**
     * 兼容已有诊断命名习惯，输出和测试都可以把 id 作为 code 使用。
     */
    public String code() {
        return id;
    }
}
