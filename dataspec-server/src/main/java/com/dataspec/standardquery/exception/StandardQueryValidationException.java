package com.dataspec.standardquery.exception;

import com.dataspec.common.sanitize.SensitiveDataSanitizer;
import com.dataspec.standardquery.model.StandardQueryValidationError;

/**
 * Standard Query DSL 专用校验异常。
 *
 * <p>该异常只用于 DSL 入口，让 API/CLI/MCP 调用方读取稳定 validationError 契约；
 * 不改变项目通用 {@code R.fail} 错误信封，避免影响其它历史接口。</p>
 */
public class StandardQueryValidationException extends RuntimeException {

    private final StandardQueryValidationError validationError;

    public StandardQueryValidationException(StandardQueryValidationError validationError) {
        super(SensitiveDataSanitizer.redactText(validationError == null ? null : validationError.message()));
        this.validationError = validationError;
    }

    public StandardQueryValidationError getValidationError() {
        return validationError;
    }
}
