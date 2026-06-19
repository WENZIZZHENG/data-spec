package com.dataspec.security.context;

import com.dataspec.security.model.ApiTokenPrincipal;

/**
 * 当前请求安全上下文。
 * <p>
 * 使用 ThreadLocal 是为了在不引入完整安全框架的前提下，把拦截器识别出的操作者传递到服务层和变更日志。
 */
public final class DataSpecSecurityContext {

    private static final ThreadLocal<ApiTokenPrincipal> CURRENT = new ThreadLocal<>();

    private DataSpecSecurityContext() {
    }

    public static void set(ApiTokenPrincipal principal) {
        CURRENT.set(principal);
    }

    public static ApiTokenPrincipal get() {
        ApiTokenPrincipal principal = CURRENT.get();
        return principal != null ? principal : ApiTokenPrincipal.local();
    }

    public static String currentOperator() {
        return get().operatorName();
    }

    public static void clear() {
        CURRENT.remove();
    }
}
