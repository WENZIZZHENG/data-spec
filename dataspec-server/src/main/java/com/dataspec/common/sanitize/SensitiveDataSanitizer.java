package com.dataspec.common.sanitize;

import com.fasterxml.jackson.databind.JsonNode;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 统一处理 DataSpec 可复制/可导出的技术 secret。
 *
 * <p>该工具只面向日志、错误、诊断、证据包和备份扫描等出口摘要，不修改业务原始输入。
 * 第一版重点覆盖 password、token、Authorization/Bearer、JDBC URL、DSN 和连接串这类明确 secret。</p>
 */
public final class SensitiveDataSanitizer {

    public static final String REDACTION = "[REDACTED]";

    private static final Pattern JDBC_PATTERN = Pattern.compile("(?i)jdbc:[^\\s\"'<>]+");
    private static final Pattern DSN_URI_PATTERN = Pattern.compile(
            "(?i)\\b((?:postgres(?:ql)?|mysql|mariadb|sqlserver|oracle|mongodb|redis)://)[^\\s\"'<>]+");
    private static final Pattern AUTHORIZATION_BEARER_PATTERN = Pattern.compile(
            "(?i)(authorization\\s*[:=]\\s*bearer\\s+)[^\\s,;]+");
    private static final Pattern AUTHORIZATION_VALUE_PATTERN = Pattern.compile(
            "(?i)(authorization\\s*[:=]\\s*)(?!\\s*[\"']?bearer\\s+)([\"']?)[^,;}&\\r\\n]+\\2");
    private static final Pattern BEARER_PATTERN = Pattern.compile("(?i)(bearer\\s+)[A-Za-z0-9._~+\\-/]+=*");
    private static final Pattern SECRET_KEY_VALUE_PATTERN = Pattern.compile(
            "(?i)((?:\"|')?\\b(?:password|passwd|pwd|token|api[_-]?token|dataspec[_-]?token|api[_-]?key|secret|client[_-]?secret|access[_-]?token|refresh[_-]?token|plain[_-]?token|token[_-]?hash|jdbc[_-]?url|connection[_-]?string|dsn)\\b(?:\"|')?\\s*[:=]\\s*)([\"']?)[^\\s\"',;}&]+\\2");
    private static final Set<String> SENSITIVE_KEYS = Set.of(
            "password",
            "passwd",
            "pwd",
            "token",
            "apitoken",
            "dataspectoken",
            "apikey",
            "authorization",
            "secret",
            "clientsecret",
            "accesstoken",
            "refreshtoken",
            "plaintoken",
            "tokenhash",
            "jdbcurl",
            "connectionstring",
            "dsn"
    );

    private SensitiveDataSanitizer() {
    }

    public static String redactText(String text) {
        return redactText(text, -1);
    }

    public static String redactText(String text, int maxLength, String... explicitSecrets) {
        if (text == null) {
            return null;
        }
        String value = redactExplicitSecrets(text, explicitSecrets);
        value = JDBC_PATTERN.matcher(value).replaceAll("jdbc:" + REDACTION);
        value = DSN_URI_PATTERN.matcher(value).replaceAll("$1" + REDACTION);
        value = AUTHORIZATION_BEARER_PATTERN.matcher(value).replaceAll("$1" + REDACTION);
        value = AUTHORIZATION_VALUE_PATTERN.matcher(value).replaceAll("$1$2" + REDACTION + "$2");
        value = BEARER_PATTERN.matcher(value).replaceAll("$1" + REDACTION);
        value = SECRET_KEY_VALUE_PATTERN.matcher(value).replaceAll("$1$2" + REDACTION + "$2");
        if (maxLength > 0 && value.length() > maxLength) {
            return value.substring(0, maxLength) + "...";
        }
        return value;
    }

    public static boolean containsSensitiveText(String text) {
        return text != null && !redactText(text).equals(text);
    }

    public static boolean isSensitiveKey(String key) {
        String normalized = normalizeKey(key);
        return SENSITIVE_KEYS.contains(normalized)
                || normalized.endsWith("token")
                || normalized.endsWith("secret")
                || normalized.contains("apikey")
                || normalized.contains("password")
                || normalized.contains("authorization")
                || normalized.contains("tokenhash")
                || normalized.contains("connectionstring")
                || normalized.contains("jdbcurl")
                || normalized.equals("dsn")
                || normalized.endsWith("dsn");
    }

    public static Object sanitizeValue(Object value) {
        return sanitizeValue(value, 0, 6);
    }

    public static Object sanitizeValue(Object value, int depth, int maxDepth) {
        if (value == null) {
            return null;
        }
        if (depth > maxDepth) {
            return "[TRUNCATED_DEPTH]";
        }
        if (value instanceof String text) {
            return redactText(text);
        }
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> result = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                String key = String.valueOf(entry.getKey());
                result.put(key, isSensitiveKey(key) ? REDACTION : sanitizeValue(entry.getValue(), depth + 1, maxDepth));
            }
            return result;
        }
        if (value instanceof Iterable<?> iterable) {
            List<Object> result = new ArrayList<>();
            for (Object item : iterable) {
                result.add(sanitizeValue(item, depth + 1, maxDepth));
            }
            return result;
        }
        if (value.getClass().isArray()) {
            int length = Array.getLength(value);
            List<Object> result = new ArrayList<>(length);
            for (int i = 0; i < length; i++) {
                result.add(sanitizeValue(Array.get(value, i), depth + 1, maxDepth));
            }
            return result;
        }
        return value;
    }

    public static boolean containsSensitiveKeyOrValue(JsonNode node) {
        return containsSensitiveKeyOrValue(node, "");
    }

    private static boolean containsSensitiveKeyOrValue(JsonNode node, String fieldName) {
        if (node == null || node.isNull()) {
            return false;
        }
        if (isSensitiveKey(fieldName)) {
            return true;
        }
        if (node.isTextual()) {
            return containsSensitiveText(node.asText(""));
        }
        if (node.isObject()) {
            var fields = node.fields();
            while (fields.hasNext()) {
                var entry = fields.next();
                if (containsSensitiveKeyOrValue(entry.getValue(), entry.getKey())) {
                    return true;
                }
            }
            return false;
        }
        if (node.isArray()) {
            for (JsonNode child : node) {
                if (containsSensitiveKeyOrValue(child, fieldName)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static String redactExplicitSecrets(String text, String... explicitSecrets) {
        String value = text;
        if (explicitSecrets == null) {
            return value;
        }
        for (String secret : explicitSecrets) {
            if (secret == null || secret.isBlank()) {
                continue;
            }
            value = Pattern.compile(Pattern.quote(secret))
                    .matcher(value)
                    .replaceAll(Matcher.quoteReplacement(REDACTION));
        }
        return value;
    }

    private static String normalizeKey(String key) {
        if (key == null) {
            return "";
        }
        return key.replaceAll("[^A-Za-z0-9]", "").toLowerCase(Locale.ROOT);
    }
}
