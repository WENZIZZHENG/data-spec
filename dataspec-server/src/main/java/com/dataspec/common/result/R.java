package com.dataspec.common.result;

import com.dataspec.common.sanitize.SensitiveDataSanitizer;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

/**
 * 统一响应封装
 */
@Data
public class R<T> {
    private int code;
    private String message;
    private T data;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private ErrorDetail error;

    public static <T> R<T> ok(T data) {
        R<T> r = new R<>();
        r.setCode(200);
        r.setMessage("success");
        r.setData(data);
        return r;
    }

    public static <T> R<T> ok() {
        return ok(null);
    }

    public static <T> R<T> fail(int code, String message) {
        R<T> r = new R<>();
        String sanitizedMessage = SensitiveDataSanitizer.redactText(message);
        r.setCode(code);
        r.setMessage(sanitizedMessage);
        r.setError(ErrorCatalog.from(code, sanitizedMessage));
        return r;
    }

    public static <T> R<T> fail(String message) {
        return fail(500, message);
    }
}
