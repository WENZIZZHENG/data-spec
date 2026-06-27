package com.dataspec.common.result;

/**
 * 面向 AI/CLI/MCP 的错误诊断字段。
 *
 * <p>顶层 {@link R#getCode()} 和 {@link R#getMessage()} 保持兼容；该对象只在失败响应中提供机器可读
 * 下一步建议，便于 agent 判断是补 token、换项目、修 SQL 还是检查数据库连接。</p>
 */
public record ErrorDetail(
        String code,
        String category,
        boolean retryable,
        String suggestedAction,
        String docsRef
) {
}
