package com.dataspec.lint.rules;

import java.util.Locale;

/**
 * 规则修复建议的轻量工具。
 *
 * 第一版只生成确定性片段，不自动改写 SQL，避免在缺少 source span 的情况下做危险替换。
 */
final class RuleFixSupport {

    private RuleFixSupport() {
    }

    static String toSnakeCase(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String snake = value
                .replaceAll("([a-z0-9])([A-Z])", "$1_$2")
                .replaceAll("[^A-Za-z0-9]+", "_")
                .toLowerCase(Locale.ROOT)
                .replaceAll("_+", "_")
                .replaceAll("^_|_$", "");
        if (!snake.isBlank() && Character.isDigit(snake.charAt(0))) {
            return "f_" + snake;
        }
        return snake;
    }

    static String requiredColumnSnippet(String columnName) {
        return switch (columnName) {
            case "id" -> "id bigserial PRIMARY KEY";
            case "created_at" -> "created_at timestamp with time zone NOT NULL DEFAULT now()";
            case "updated_at" -> "updated_at timestamp with time zone NOT NULL DEFAULT now()";
            case "is_deleted" -> "is_deleted boolean NOT NULL DEFAULT false";
            default -> columnName + " text";
        };
    }
}
