package com.dataspec.lint.engine;

import com.dataspec.lint.model.LintIssue;
import com.dataspec.lint.model.Severity;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * SQL issue source span 定位测试。
 */
class SqlIssueSourceSpanResolverTest {

    private final SqlIssueSourceSpanResolver resolver = new SqlIssueSourceSpanResolver();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void resolvesColumnRangeInsideMatchingTableWhenColumnNamesOverlap() {
        String sql = """
                CREATE TABLE first_table (
                    shared_id bigint
                );

                CREATE TABLE second_table (
                    shared_id varchar(64)
                );
                """;
        LintIssue issue = issue("field_suffix_type", "second_table", "shared_id");

        resolver.resolve(sql, List.of(issue));

        assertEquals(6, issue.getLine());
        assertEquals(5, issue.getColumn());
        assertEquals("shared_id", sql.substring(issue.getSourceStart(), issue.getSourceEnd()));
        Map<String, Object> map = issueMap(issue);
        assertEquals(6, map.get("lineEnd"));
        assertEquals(14, map.get("columnEnd"));
        assertEquals("column", map.get("locationKind"));
    }

    @Test
    void resolvesQuotedSchemaAndBracketIdentifiers() {
        String sql = """
                CREATE TABLE sales."UserOrder" (
                    [badName] bigint
                );
                """;
        LintIssue issue = issue("field_naming_snake_case", "UserOrder", "badName");

        resolver.resolve(sql, List.of(issue));

        assertEquals(2, issue.getLine());
        assertEquals(5, issue.getColumn());
        assertEquals("[badName]", sql.substring(issue.getSourceStart(), issue.getSourceEnd()));
        Map<String, Object> map = issueMap(issue);
        assertEquals(2, map.get("lineEnd"));
        assertEquals(14, map.get("columnEnd"));
        assertEquals("column", map.get("locationKind"));
    }

    @Test
    void resolvesCommentOnColumnToColumnIdentifier() {
        String sql = "COMMENT ON COLUMN public.orders.legacy_name IS '旧字段';";
        LintIssue issue = issue("comment_missing", "orders", "legacy_name");

        resolver.resolve(sql, List.of(issue));

        assertEquals(1, issue.getLine());
        assertEquals("legacy_name", sql.substring(issue.getSourceStart(), issue.getSourceEnd()));
        Map<String, Object> map = issueMap(issue);
        assertEquals(1, map.get("lineEnd"));
        assertEquals("comment_column", map.get("locationKind"));
    }

    @Test
    void leavesUnresolvedIssueWithoutRangeMetadata() {
        LintIssue issue = issue("synthetic_unresolved", null, null);

        resolver.resolve("CREATE TABLE users (id bigint);", List.of(issue));

        assertNull(issue.getLine());
        assertNull(issue.getColumn());
        assertNull(issue.getSourceStart());
        assertNull(issue.getSourceEnd());
        Map<String, Object> map = issueMap(issue);
        assertNull(map.get("lineEnd"));
        assertNull(map.get("columnEnd"));
        assertNull(map.get("locationKind"));
    }

    private LintIssue issue(String ruleCode, String tableName, String columnName) {
        return LintIssue.builder()
                .severity(Severity.WARNING)
                .ruleCode(ruleCode)
                .ruleName(ruleCode)
                .message(ruleCode)
                .tableName(tableName)
                .columnName(columnName)
                .build();
    }

    private Map<String, Object> issueMap(LintIssue issue) {
        return objectMapper.convertValue(issue, new TypeReference<>() {
        });
    }
}
