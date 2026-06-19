package com.dataspec.lint.engine;

import com.dataspec.lint.model.LintIssue;

import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 基于原始 SQL 文本为 lint issue 回填第一版 source span。
 */
class SqlIssueSourceSpanResolver {

    private static final Pattern CREATE_TABLE_PATTERN = Pattern.compile(
            "\\bCREATE\\s+TABLE\\s+(?:IF\\s+NOT\\s+EXISTS\\s+)?([^\\s(]+)",
            Pattern.CASE_INSENSITIVE);

    void resolve(String sql, List<LintIssue> issues) {
        if (sql == null || sql.isBlank() || issues == null || issues.isEmpty()) {
            return;
        }
        for (LintIssue issue : issues) {
            if (issue == null || issue.getSourceStart() != null) {
                continue;
            }
            SourceSpan span = findSpan(sql, issue);
            if (span == null) {
                continue;
            }
            SourcePosition position = toPosition(sql, span.start());
            issue.setLine(position.line());
            issue.setColumn(position.column());
            issue.setSourceStart(span.start());
            issue.setSourceEnd(span.end());
        }
    }

    private SourceSpan findSpan(String sql, LintIssue issue) {
        if (!isBlank(issue.getColumnName())) {
            SourceSpan columnSpan = findColumnSpan(sql, issue.getTableName(), issue.getColumnName());
            if (columnSpan != null) {
                return columnSpan;
            }
        }
        if (!isBlank(issue.getTableName())) {
            SourceSpan tableSpan = findTableSpan(sql, issue.getTableName());
            if (tableSpan != null) {
                return tableSpan;
            }
        }
        if (!isBlank(issue.getColumnName())) {
            return findIdentifierAnywhere(sql, issue.getColumnName());
        }
        return null;
    }

    private SourceSpan findColumnSpan(String sql, String tableName, String columnName) {
        SourceSpan tableRange = isBlank(tableName) ? null : findTableRange(sql, tableName);
        int searchStart = tableRange == null ? 0 : tableRange.start();
        int searchEnd = tableRange == null ? sql.length() : tableRange.end();
        String scope = sql.substring(searchStart, searchEnd);

        Matcher matcher = leadingIdentifierPattern(columnName).matcher(scope);
        if (matcher.find()) {
            return new SourceSpan(searchStart + matcher.start(1), searchStart + matcher.end(1));
        }
        return null;
    }

    private SourceSpan findTableSpan(String sql, String tableName) {
        Matcher matcher = CREATE_TABLE_PATTERN.matcher(sql);
        while (matcher.find()) {
            String token = matcher.group(1);
            if (!lastQualifiedPart(token).equalsIgnoreCase(tableName)) {
                continue;
            }
            SourceSpan relativeSpan = findIdentifierInToken(token, tableName);
            if (relativeSpan != null) {
                return new SourceSpan(matcher.start(1) + relativeSpan.start(), matcher.start(1) + relativeSpan.end());
            }
        }
        return findIdentifierAnywhere(sql, tableName);
    }

    private SourceSpan findTableRange(String sql, String tableName) {
        Matcher matcher = CREATE_TABLE_PATTERN.matcher(sql);
        while (matcher.find()) {
            String token = matcher.group(1);
            if (!lastQualifiedPart(token).equalsIgnoreCase(tableName)) {
                continue;
            }
            int end = sql.indexOf(';', matcher.end());
            return new SourceSpan(matcher.start(), end >= 0 ? end + 1 : sql.length());
        }
        return null;
    }

    private SourceSpan findIdentifierAnywhere(String sql, String identifier) {
        Matcher matcher = identifierPattern(identifier).matcher(sql);
        if (matcher.find()) {
            return new SourceSpan(matcher.start(1), matcher.end(1));
        }
        return null;
    }

    private Pattern leadingIdentifierPattern(String identifier) {
        return Pattern.compile(
                "(?im)^\\s*(\"" + Pattern.quote(identifier) + "\"|`" + Pattern.quote(identifier)
                        + "`|\\[" + Pattern.quote(identifier) + "\\]|" + Pattern.quote(identifier)
                        + ")(?=$|\\s|,|\\(|\\))");
    }

    private Pattern identifierPattern(String identifier) {
        return Pattern.compile(
                "(?i)(?<![A-Za-z0-9_])(\"" + Pattern.quote(identifier) + "\"|`"
                        + Pattern.quote(identifier) + "`|\\[" + Pattern.quote(identifier) + "\\]|"
                        + Pattern.quote(identifier) + ")(?![A-Za-z0-9_])");
    }

    private SourceSpan findIdentifierInToken(String token, String identifier) {
        String lowerToken = token.toLowerCase(Locale.ROOT);
        String lowerIdentifier = identifier.toLowerCase(Locale.ROOT);
        for (String variant : List.of("\"" + lowerIdentifier + "\"", "`" + lowerIdentifier + "`",
                "[" + lowerIdentifier + "]", lowerIdentifier)) {
            int index = lowerToken.indexOf(variant);
            if (index >= 0) {
                return new SourceSpan(index, index + variant.length());
            }
        }
        return null;
    }

    private String lastQualifiedPart(String token) {
        if (token == null || token.isBlank()) {
            return "";
        }
        String[] parts = token.split("\\.");
        return stripIdentifierQuotes(parts[parts.length - 1]);
    }

    private String stripIdentifierQuotes(String value) {
        String result = value == null ? "" : value.trim();
        while ((result.startsWith("\"") && result.endsWith("\""))
                || (result.startsWith("`") && result.endsWith("`"))
                || (result.startsWith("[") && result.endsWith("]"))) {
            result = result.substring(1, result.length() - 1).trim();
        }
        return result;
    }

    private SourcePosition toPosition(String sql, int offset) {
        int line = 1;
        int lineStart = 0;
        for (int i = 0; i < offset && i < sql.length(); i++) {
            if (sql.charAt(i) == '\n') {
                line++;
                lineStart = i + 1;
            }
        }
        return new SourcePosition(line, offset - lineStart + 1);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private record SourceSpan(int start, int end) {
    }

    private record SourcePosition(int line, int column) {
    }
}
