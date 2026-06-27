package com.dataspec.aicontext.model;

import java.util.Locale;
import java.util.Set;

/**
 * AI Context 裁剪参数。
 *
 * <p>所有入口都使用同一个模型，避免 API、CLI、MCP 对 scope 语义各自实现后漂移。</p>
 */
public record AiContextScopeOptions(
        String scope,
        String query,
        String status,
        Integer limit
) {
    private static final String DEFAULT_SCOPE = "all";
    private static final Set<String> SUPPORTED_SCOPES = Set.of("all", "field", "domain", "tag", "table", "changed");
    private static final int MAX_LIMIT = 500;

    public AiContextScopeOptions {
        scope = normalizeScope(scope);
        query = normalizeText(query);
        status = normalizeText(status);
        if (status != null) {
            status = status.toLowerCase(Locale.ROOT);
        }
        limit = normalizeLimit(limit);
    }

    public static AiContextScopeOptions full() {
        return new AiContextScopeOptions(null, null, null, null);
    }

    public boolean scoped() {
        return !DEFAULT_SCOPE.equals(scope) || query != null || status != null || limit != null;
    }

    public boolean scopeSupported() {
        return SUPPORTED_SCOPES.contains(scope);
    }

    private static String normalizeScope(String value) {
        String normalized = normalizeText(value);
        if (normalized == null) {
            return DEFAULT_SCOPE;
        }
        return normalized.toLowerCase(Locale.ROOT);
    }

    private static String normalizeText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static Integer normalizeLimit(Integer value) {
        if (value == null || value <= 0) {
            return null;
        }
        return Math.min(value, MAX_LIMIT);
    }
}
