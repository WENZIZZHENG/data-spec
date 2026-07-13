package com.dataspec.querynormalization.tokenizer;

import com.dataspec.querynormalization.model.NameLexicalToken;
import com.dataspec.querynormalization.model.QueryTokenKind;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 无外部运行时和项目数据依赖的确定性名称 tokenizer。
 *
 * <p>拆分顺序固定为分隔符、汉字/字母/数字边界、acronym 边界和普通 camel 边界。
 * 单位只从有界集合分类，不把相似 substring 当作单位。</p>
 */
@Component
public class NameLexicalTokenizer {

    /** 单次内部解析最多读取的 Unicode code point 数，避免超长输入放大 CPU 和内存。 */
    public static final int MAX_INPUT_LENGTH = 512;
    /** API、CLI、MCP 和前端最多接收的 query token evidence 数量。 */
    public static final int MAX_EVIDENCE_TOKEN_COUNT = 32;
    /** 单条 query token evidence 文本的最大字符长度；内部匹配值不使用该上限。 */
    public static final int MAX_EVIDENCE_TOKEN_LENGTH = 64;

    private static final Set<String> UNITS = Set.of(
            "ms", "s", "min", "h", "d",
            "b", "kb", "mb", "gb", "tb",
            "mm", "cm", "m", "km",
            "mg", "g", "kg", "ml", "l",
            "pct", "percent", "usd", "cny");

    /**
     * 按固定边界拆分名称；输入按 Unicode code point 确定性限长，内部 token 保留完整值。
     * API evidence 的数量和文本长度由 normalization 出口单独限制，避免截断值参与精确匹配。
     *
     * @param value 待拆分名称，可为空
     * @return 不可变、有序 token 列表
     */
    public List<NameLexicalToken> tokenize(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        String bounded = boundByCodePoints(value);
        int[] codePoints = bounded.codePoints().toArray();
        List<NameLexicalToken> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (int index = 0; index < codePoints.length; index++) {
            int character = codePoints[index];
            if (!isTokenCharacter(character)) {
                flush(current, result);
                continue;
            }
            int previous = current.isEmpty() ? 0 : current.codePointBefore(current.length());
            int next = index + 1 < codePoints.length ? codePoints[index + 1] : 0;
            if (!current.isEmpty() && isBoundary(previous, character, next)) {
                flush(current, result);
            }
            current.appendCodePoint(character);
        }
        flush(current, result);
        return List.copyOf(result);
    }

    private String boundByCodePoints(String value) {
        int codePointCount = value.codePointCount(0, value.length());
        if (codePointCount <= MAX_INPUT_LENGTH) {
            return value;
        }
        return value.substring(0, value.offsetByCodePoints(0, MAX_INPUT_LENGTH));
    }

    private boolean isBoundary(int previous, int current, int next) {
        CharacterClass previousClass = characterClass(previous);
        CharacterClass currentClass = characterClass(current);
        if (previousClass != currentClass) {
            return true;
        }
        if (currentClass != CharacterClass.LETTER) {
            return false;
        }
        if (Character.isLowerCase(previous) && Character.isUpperCase(current)) {
            return true;
        }
        return Character.isUpperCase(previous)
                && Character.isUpperCase(current)
                && next != 0
                && Character.isLowerCase(next);
    }

    private void flush(StringBuilder current, List<NameLexicalToken> result) {
        if (current.isEmpty()) {
            current.setLength(0);
            return;
        }
        String text = current.toString();
        String normalized = text.toLowerCase(Locale.ROOT);
        result.add(new NameLexicalToken(text, normalized, kind(text, normalized)));
        current.setLength(0);
    }

    private QueryTokenKind kind(String text, String normalized) {
        if (text.codePoints().allMatch(Character::isDigit)) {
            return QueryTokenKind.NUMBER;
        }
        if (text.codePoints().allMatch(NameLexicalTokenizer::isHan)) {
            return QueryTokenKind.HAN;
        }
        if (UNITS.contains(normalized)) {
            return QueryTokenKind.UNIT;
        }
        if (text.codePointCount(0, text.length()) > 1 && text.codePoints().allMatch(character ->
                !Character.isLetter(character) || Character.isUpperCase(character))) {
            return QueryTokenKind.ACRONYM;
        }
        return QueryTokenKind.WORD;
    }

    private boolean isTokenCharacter(int character) {
        return Character.isLetterOrDigit(character) || isHan(character);
    }

    private CharacterClass characterClass(int character) {
        if (isHan(character)) {
            return CharacterClass.HAN;
        }
        if (Character.isDigit(character)) {
            return CharacterClass.NUMBER;
        }
        return CharacterClass.LETTER;
    }

    private static boolean isHan(int codePoint) {
        return Character.UnicodeScript.of(codePoint) == Character.UnicodeScript.HAN;
    }

    private enum CharacterClass {
        HAN,
        LETTER,
        NUMBER
    }
}
