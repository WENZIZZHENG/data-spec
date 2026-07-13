package com.dataspec.querynormalization.model;

/**
 * 纯词法拆分结果。
 *
 * @param text       有界输入内的完整原始片段；仅用于安全输出前的内部处理
 * @param normalized 使用 Locale.ROOT 小写后的稳定 token
 * @param kind       token 的确定性边界类型
 */
public record NameLexicalToken(
        String text,
        String normalized,
        QueryTokenKind kind
) {
}
