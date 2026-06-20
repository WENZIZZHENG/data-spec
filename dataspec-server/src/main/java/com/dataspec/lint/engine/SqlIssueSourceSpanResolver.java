package com.dataspec.lint.engine;

import com.dataspec.lint.model.LintIssue;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 基于原始 SQL 文本为 lint issue 回填 source range。
 */
class SqlIssueSourceSpanResolver {

    private static final String IDENTIFIER_TOKEN = "(?:\"[^\"]+\"|`[^`]+`|\\[[^\\]]+\\]|[A-Za-z_][A-Za-z0-9_$]*)";

    private static final Pattern CREATE_TABLE_PATTERN = Pattern.compile(
            "\\bCREATE\\s+TABLE\\s+(?:IF\\s+NOT\\s+EXISTS\\s+)?("
                    + IDENTIFIER_TOKEN + "(?:\\s*\\.\\s*" + IDENTIFIER_TOKEN + ")*)",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern COMMENT_ON_COLUMN_PATTERN = Pattern.compile(
            "\\bCOMMENT\\s+ON\\s+COLUMN\\s+(" + IDENTIFIER_TOKEN
                    + "(?:\\s*\\.\\s*" + IDENTIFIER_TOKEN + ")+)\\s+IS\\b",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern COMMENT_ON_TABLE_PATTERN = Pattern.compile(
            "\\bCOMMENT\\s+ON\\s+TABLE\\s+(" + IDENTIFIER_TOKEN
                    + "(?:\\s*\\.\\s*" + IDENTIFIER_TOKEN + ")*)\\s+IS\\b",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern IDENTIFIER_EXTRACT_PATTERN = Pattern.compile(IDENTIFIER_TOKEN);

    void resolve(String sql, List<LintIssue> issues) {
        if (sql == null || sql.isBlank() || issues == null || issues.isEmpty()) {
            return;
        }
        List<TableRange> tableRanges = findTableRanges(sql);
        for (LintIssue issue : issues) {
            if (issue == null || issue.getSourceStart() != null) {
                continue;
            }
            SourceSpan span = findSpan(sql, issue, tableRanges);
            if (span == null) {
                continue;
            }
            SourcePosition position = toPosition(sql, span.start());
            SourcePosition endPosition = toPosition(sql, span.end());
            issue.setLine(position.line());
            issue.setColumn(position.column());
            issue.setLineEnd(endPosition.line());
            issue.setColumnEnd(endPosition.column());
            issue.setSourceStart(span.start());
            issue.setSourceEnd(span.end());
            issue.setLocationKind(span.kind());
        }
    }

    private SourceSpan findSpan(String sql, LintIssue issue, List<TableRange> tableRanges) {
        if (!isBlank(issue.getColumnName())) {
            SourceSpan columnSpan = findColumnSpan(sql, tableRanges, issue.getTableName(), issue.getColumnName());
            if (columnSpan != null) {
                return columnSpan;
            }
            SourceSpan commentColumnSpan = findCommentColumnSpan(sql, issue.getTableName(), issue.getColumnName());
            if (commentColumnSpan != null) {
                return commentColumnSpan;
            }
        }
        if (!isBlank(issue.getTableName())) {
            SourceSpan commentTableSpan = findCommentTableSpan(sql, issue.getTableName());
            if (commentTableSpan != null && isCommentRule(issue)) {
                return commentTableSpan;
            }
            SourceSpan tableSpan = findTableSpan(sql, tableRanges, issue.getTableName());
            if (tableSpan != null) {
                return tableSpan;
            }
        }
        if (!isBlank(issue.getColumnName())) {
            return findIdentifierAnywhere(sql, issue.getColumnName());
        }
        return null;
    }

    private SourceSpan findColumnSpan(String sql, List<TableRange> tableRanges, String tableName, String columnName) {
        List<TableRange> candidates = isBlank(tableName)
                ? tableRanges
                : tableRanges.stream()
                .filter(range -> range.matches(tableName))
                .toList();
        for (TableRange range : candidates) {
            SourceSpan span = findLeadingIdentifierInRange(sql, range.start(), range.end(), columnName, "column");
            if (span != null) {
                return span;
            }
        }
        return null;
    }

    private SourceSpan findTableSpan(String sql, List<TableRange> tableRanges, String tableName) {
        for (TableRange range : tableRanges) {
            if (range.matches(tableName)) {
                return range.tableSpan();
            }
        }
        return findIdentifierAnywhere(sql, tableName);
    }

    private List<TableRange> findTableRanges(String sql) {
        List<TableRange> ranges = new ArrayList<>();
        Matcher matcher = CREATE_TABLE_PATTERN.matcher(sql);
        while (matcher.find()) {
            String token = matcher.group(1);
            String tableName = lastQualifiedPart(token);
            SourceSpan relativeSpan = findIdentifierInToken(token, tableName, "table");
            if (relativeSpan == null) {
                continue;
            }
            int end = sql.indexOf(';', matcher.end());
            ranges.add(new TableRange(
                    tableName,
                    new SourceSpan(matcher.start(1) + relativeSpan.start(), matcher.start(1) + relativeSpan.end(), "table"),
                    matcher.start(),
                    end >= 0 ? end + 1 : sql.length()));
        }
        return ranges;
    }

    private SourceSpan findCommentColumnSpan(String sql, String tableName, String columnName) {
        Matcher matcher = COMMENT_ON_COLUMN_PATTERN.matcher(sql);
        while (matcher.find()) {
            List<IdentifierPart> parts = identifierParts(matcher.group(1));
            if (parts.size() < 2) {
                continue;
            }
            IdentifierPart columnPart = parts.get(parts.size() - 1);
            IdentifierPart tablePart = parts.get(parts.size() - 2);
            if (!columnPart.matches(columnName) || (!isBlank(tableName) && !tablePart.matches(tableName))) {
                continue;
            }
            return new SourceSpan(
                    matcher.start(1) + columnPart.start(),
                    matcher.start(1) + columnPart.end(),
                    "comment_column");
        }
        return null;
    }

    private SourceSpan findCommentTableSpan(String sql, String tableName) {
        Matcher matcher = COMMENT_ON_TABLE_PATTERN.matcher(sql);
        while (matcher.find()) {
            List<IdentifierPart> parts = identifierParts(matcher.group(1));
            if (parts.isEmpty()) {
                continue;
            }
            IdentifierPart tablePart = parts.get(parts.size() - 1);
            if (tablePart.matches(tableName)) {
                return new SourceSpan(
                        matcher.start(1) + tablePart.start(),
                        matcher.start(1) + tablePart.end(),
                        "comment_table");
            }
        }
        return null;
    }

    private SourceSpan findIdentifierAnywhere(String sql, String identifier) {
        Matcher matcher = identifierPattern(identifier).matcher(sql);
        if (matcher.find()) {
            return new SourceSpan(matcher.start(1), matcher.end(1), "identifier");
        }
        return null;
    }

    private SourceSpan findLeadingIdentifierInRange(String sql, int start, int end, String identifier, String kind) {
        int lineStart = start;
        while (lineStart < end) {
            int lineEnd = sql.indexOf('\n', lineStart);
            if (lineEnd < 0 || lineEnd > end) {
                lineEnd = end;
            }
            int tokenStart = lineStart;
            while (tokenStart < lineEnd && Character.isWhitespace(sql.charAt(tokenStart))) {
                tokenStart++;
            }
            SourceSpan span = matchIdentifierAt(sql, tokenStart, identifier, kind);
            if (span != null) {
                return span;
            }
            lineStart = lineEnd + 1;
        }
        return null;
    }

    private Pattern identifierPattern(String identifier) {
        return Pattern.compile(
                "(?i)(?<![A-Za-z0-9_])(\"" + Pattern.quote(identifier) + "\"|`"
                        + Pattern.quote(identifier) + "`|\\[" + Pattern.quote(identifier) + "\\]|"
                        + Pattern.quote(identifier) + ")(?![A-Za-z0-9_])");
    }

    private SourceSpan matchIdentifierAt(String sql, int start, String identifier, String kind) {
        if (start < 0 || start >= sql.length() || isBlank(identifier)) {
            return null;
        }
        char first = sql.charAt(start);
        int end;
        if (first == '"') {
            end = sql.indexOf('"', start + 1);
            end = end < 0 ? -1 : end + 1;
        } else if (first == '`') {
            end = sql.indexOf('`', start + 1);
            end = end < 0 ? -1 : end + 1;
        } else if (first == '[') {
            end = sql.indexOf(']', start + 1);
            end = end < 0 ? -1 : end + 1;
        } else {
            end = start;
            while (end < sql.length()) {
                char ch = sql.charAt(end);
                if (Character.isLetterOrDigit(ch) || ch == '_' || ch == '$') {
                    end++;
                } else {
                    break;
                }
            }
        }
        if (end <= start) {
            return null;
        }
        String token = sql.substring(start, end);
        if (stripIdentifierQuotes(token).equalsIgnoreCase(identifier)) {
            return new SourceSpan(start, end, kind);
        }
        return null;
    }

    private SourceSpan findIdentifierInToken(String token, String identifier, String kind) {
        for (IdentifierPart part : identifierParts(token)) {
            if (part.matches(identifier)) {
                return new SourceSpan(part.start(), part.end(), kind);
            }
        }
        return null;
    }

    private List<IdentifierPart> identifierParts(String token) {
        List<IdentifierPart> parts = new ArrayList<>();
        Matcher matcher = IDENTIFIER_EXTRACT_PATTERN.matcher(token);
        while (matcher.find()) {
            parts.add(new IdentifierPart(matcher.group(), matcher.start(), matcher.end()));
        }
        return parts;
    }

    private String lastQualifiedPart(String token) {
        List<IdentifierPart> parts = identifierParts(token == null ? "" : token);
        if (parts.isEmpty()) {
            return "";
        }
        return stripIdentifierQuotes(parts.get(parts.size() - 1).token());
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

    private boolean isCommentRule(LintIssue issue) {
        String code = issue.getRuleCode();
        return code != null && code.toLowerCase(Locale.ROOT).contains("comment");
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

    private record SourceSpan(int start, int end, String kind) {
    }

    private record TableRange(String tableName, SourceSpan tableSpan, int start, int end) {
        boolean matches(String value) {
            return tableName != null && tableName.equalsIgnoreCase(value);
        }
    }

    private record IdentifierPart(String token, int start, int end) {
        boolean matches(String value) {
            return stripStatic(token).equalsIgnoreCase(value);
        }

        private static String stripStatic(String value) {
            String result = value == null ? "" : value.trim();
            while ((result.startsWith("\"") && result.endsWith("\""))
                    || (result.startsWith("`") && result.endsWith("`"))
                    || (result.startsWith("[") && result.endsWith("]"))) {
                result = result.substring(1, result.length() - 1).trim();
            }
            return result;
        }
    }

    private record SourcePosition(int line, int column) {
    }
}
