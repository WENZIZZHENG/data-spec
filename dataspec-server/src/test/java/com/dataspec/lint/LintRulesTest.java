package com.dataspec.lint;

import com.dataspec.lint.model.*;
import com.dataspec.lint.engine.SqlParserService;
import com.dataspec.lint.rules.*;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 规则引擎单元测试（不依赖 Spring 容器）
 */
class LintRulesTest {

    private String readExample(String fileName) throws Exception {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            Path candidate = current.resolve("examples").resolve(fileName);
            if (Files.exists(candidate)) {
                return Files.readString(candidate);
            }
            current = current.getParent();
        }
        throw new IllegalStateException("未找到 examples/" + fileName);
    }

    // 构造测试表定义
    private TableDef goodTable() {
        return TableDef.builder()
                .name("user_order")
                .comment("用户订单表")
                .columns(List.of(
                        ColumnDef.builder().name("id").dataType("bigserial").nullable(false).comment("主键").build(),
                        ColumnDef.builder().name("user_id").dataType("bigint").nullable(false).comment("用户ID").build(),
                        ColumnDef.builder().name("order_no").dataType("varchar(64)").nullable(false).comment("订单号").build(),
                        ColumnDef.builder().name("amount_cent").dataType("bigint").nullable(false).comment("金额（分）").build(),
                        ColumnDef.builder().name("created_at").dataType("timestamp with time zone").nullable(false).comment("创建时间").build(),
                        ColumnDef.builder().name("updated_at").dataType("timestamp with time zone").nullable(false).comment("更新时间").build(),
                        ColumnDef.builder().name("is_deleted").dataType("boolean").nullable(false).comment("软删除").build()
                ))
                .build();
    }

    private TableDef badTable() {
        return TableDef.builder()
                .name("UserOrder")
                .columns(List.of(
                        ColumnDef.builder().name("userId").dataType("bigint").nullable(false).build(),
                        ColumnDef.builder().name("uid").dataType("bigint").nullable(false).build(),
                        ColumnDef.builder().name("create_time").dataType("datetime").nullable(true).build(),
                        ColumnDef.builder().name("totalAmount").dataType("float").nullable(false).build()
                ))
                .build();
    }

    private RuleContext contextOf(TableDef... tables) {
        return RuleContext.builder()
                .tables(List.of(tables))
                .ruleParams(Map.of())
                .build();
    }

    @Test
    void snakeCaseRule_goodTable_noIssues() {
        var rule = new FieldNamingSnakeCaseRule();
        List<LintIssue> issues = rule.check(contextOf(goodTable()));
        assertTrue(issues.isEmpty(), "规范表不应有 snake_case 问题");
    }

    @Test
    void snakeCaseRule_badTable_hasIssues() {
        var rule = new FieldNamingSnakeCaseRule();
        List<LintIssue> issues = rule.check(contextOf(badTable()));
        assertFalse(issues.isEmpty(), "不规范表应有 snake_case 问题");
        // UserOrder, userId, totalAmount 都不符合 snake_case（注：uid 和 create_time 符合 snake_case）
        assertTrue(issues.size() >= 2);
        LintIssue userIdIssue = issues.stream()
                .filter(i -> i.getColumnName().equals("userId"))
                .findFirst()
                .orElseThrow();
        assertEquals("userId", userIdIssue.getBefore());
        assertEquals("user_id", userIdIssue.getReplacement());
        assertEquals("user_id", userIdIssue.getAfter());
        assertTrue(userIdIssue.getConfidence() >= 80);
    }

    @Test
    void tableNameSnakeCaseRule_goodTable_noIssues() {
        var rule = new TableNameSnakeCaseRule();
        List<LintIssue> issues = rule.check(contextOf(goodTable()));
        assertTrue(issues.isEmpty(), "规范表名不应有 snake_case 问题");
    }

    @Test
    void tableNameSnakeCaseRule_badTable_hasIssue() {
        var rule = new TableNameSnakeCaseRule();
        List<LintIssue> issues = rule.check(contextOf(badTable()));
        assertEquals(1, issues.size());
        assertEquals("table_naming_snake_case", issues.get(0).getRuleCode());
        assertEquals("UserOrder", issues.get(0).getTableName());
        assertEquals("UserOrder", issues.get(0).getBefore());
        assertEquals("user_order", issues.get(0).getReplacement());
        assertEquals("user_order", issues.get(0).getAfter());
    }

    @Test
    void forbiddenFieldRule_detectsUid() {
        var rule = new ForbiddenFieldNameRule();
        List<LintIssue> issues = rule.check(contextOf(badTable()));
        assertTrue(issues.stream().anyMatch(i -> i.getColumnName().equals("uid")),
                "uid 应被检测为禁用字段名");
        assertTrue(issues.stream().anyMatch(i -> i.getColumnName().equals("create_time")),
                "create_time 应被检测为禁用字段名");
        LintIssue uidIssue = issues.stream()
                .filter(i -> i.getColumnName().equals("uid"))
                .findFirst()
                .orElseThrow();
        assertEquals("uid", uidIssue.getBefore());
        assertEquals("user_id", uidIssue.getReplacement());
        assertEquals("user_id", uidIssue.getAfter());
        assertTrue(uidIssue.getSuggestion().contains("user_id"));
    }

    @Test
    void recommendedFieldRule_suggestsCreatedAt() {
        var rule = new RecommendedFieldNameRule();
        List<LintIssue> issues = rule.check(contextOf(badTable()));
        assertTrue(issues.stream().anyMatch(i ->
                i.getColumnName().equals("create_time")
                        && i.getMessage().contains("created_at")),
                "create_time 应建议改为 created_at");
        LintIssue issue = issues.stream()
                .filter(i -> i.getColumnName().equals("create_time"))
                .findFirst()
                .orElseThrow();
        assertEquals("create_time", issue.getBefore());
        assertEquals("created_at", issue.getReplacement());
        assertEquals("created_at", issue.getAfter());
        assertTrue(issue.getConfidence() >= 90);
    }

    @Test
    void requiredColumnsRule_goodTable_noIssues() {
        var rule = new RequiredColumnsRule();
        List<LintIssue> issues = rule.check(contextOf(goodTable()));
        assertTrue(issues.isEmpty(), "规范表应包含所有必含列");
    }

    @Test
    void requiredColumnsRule_badTable_missingColumns() {
        var rule = new RequiredColumnsRule();
        List<LintIssue> issues = rule.check(contextOf(badTable()));
        // 缺少 id, created_at, updated_at, is_deleted
        assertEquals(4, issues.size());
        LintIssue createdAt = issues.stream()
                .filter(i -> "created_at".equals(i.getReplacement()))
                .findFirst()
                .orElseThrow();
        assertTrue(createdAt.getSuggestion().contains("补充必含列"));
        assertTrue(createdAt.getAfter().contains("created_at"));
    }

    @Test
    void amountFieldRule_detectsFloat() {
        var rule = new AmountFieldRule();
        List<LintIssue> issues = rule.check(contextOf(badTable()));
        assertTrue(issues.stream().anyMatch(i ->
                i.getColumnName().equals("totalAmount")
                        && i.getMessage().contains("float")),
                "totalAmount 使用 float 应被警告");
    }

    @Test
    void amountFieldRule_goodTable_noIssues() {
        var rule = new AmountFieldRule();
        List<LintIssue> issues = rule.check(contextOf(goodTable()));
        assertTrue(issues.isEmpty(), "bigint 金额字段不应有问题");
    }

    @Test
    void fieldSuffixTypeRule_goodTable_noIssues() {
        var rule = new FieldSuffixTypeRule();
        List<LintIssue> issues = rule.check(contextOf(goodTable()));
        assertTrue(issues.isEmpty(), "规范后缀/前缀字段类型不应有问题");
    }

    @Test
    void fieldSuffixTypeRule_detectsMismatchedTypes() {
        var rule = new FieldSuffixTypeRule();
        TableDef table = TableDef.builder()
                .name("user_order")
                .columns(List.of(
                        ColumnDef.builder().name("user_id").dataType("varchar(64)").build(),
                        ColumnDef.builder().name("paid_at").dataType("varchar(32)").build(),
                        ColumnDef.builder().name("order_no").dataType("bigint").build(),
                        ColumnDef.builder().name("item_count").dataType("varchar(10)").build(),
                        ColumnDef.builder().name("is_paid").dataType("integer").build()
                ))
                .build();

        List<LintIssue> issues = rule.check(contextOf(table));

        assertEquals(5, issues.size());
        assertTrue(issues.stream().allMatch(issue -> "field_suffix_type".equals(issue.getRuleCode())));
        LintIssue userId = issues.stream()
                .filter(issue -> "user_id".equals(issue.getColumnName()))
                .findFirst()
                .orElseThrow();
        assertEquals("varchar(64)", userId.getBefore());
        assertEquals("bigint", userId.getReplacement());
        assertEquals("bigint", userId.getAfter());
        assertTrue(userId.getConfidence() >= 70);
    }

    @Test
    void fieldSuffixTypeRule_acceptsMySqlUnsignedAndTinyintBoolean() {
        var rule = new FieldSuffixTypeRule();
        TableDef table = TableDef.builder()
                .name("user_order")
                .columns(List.of(
                        ColumnDef.builder().name("user_id").dataType("bigint unsigned").build(),
                        ColumnDef.builder().name("item_count").dataType("int unsigned").build(),
                        ColumnDef.builder().name("is_paid").dataType("tinyint(1)").build(),
                        ColumnDef.builder().name("is_deleted").dataType("tinyint").build()
                ))
                .build();

        List<LintIssue> issues = rule.check(contextOf(table));

        assertTrue(issues.isEmpty(), "MySQL unsigned 数字类型和 tinyint(1) 布尔习惯不应误报");
    }

    @Test
    void fieldSuffixTypeRule_customParams() {
        var rule = new FieldSuffixTypeRule();
        TableDef table = TableDef.builder()
                .name("order")
                .columns(List.of(ColumnDef.builder().name("order_code").dataType("bigint").build()))
                .build();
        RuleContext ctx = RuleContext.builder()
                .tables(List.of(table))
                .ruleParams(Map.of("suffixTypes", Map.of("_code", List.of("varchar"))))
                .build();

        List<LintIssue> issues = rule.check(ctx);

        assertEquals(1, issues.size());
        assertEquals("order_code", issues.get(0).getColumnName());
    }

    @Test
    void fieldSuffixTypeRule_ignoresEmptyCustomTypeList() {
        var rule = new FieldSuffixTypeRule();
        TableDef table = TableDef.builder()
                .name("order")
                .columns(List.of(ColumnDef.builder().name("user_id").dataType("varchar(64)").build()))
                .build();
        RuleContext ctx = RuleContext.builder()
                .tables(List.of(table))
                .ruleParams(Map.of("suffixTypes", Map.of("_id", List.of())))
                .build();

        List<LintIssue> issues = rule.check(ctx);

        assertTrue(issues.isEmpty());
    }

    @Test
    void commentMissingRule_goodTable_noIssues() {
        var rule = new CommentMissingRule();
        List<LintIssue> issues = rule.check(contextOf(goodTable()));
        assertTrue(issues.isEmpty(), "所有字段都有注释不应有问题");
    }

    @Test
    void commentMissingRule_badTable_hasIssues() {
        var rule = new CommentMissingRule();
        List<LintIssue> issues = rule.check(contextOf(badTable()));
        // 表没有注释 + 所有字段没有注释
        assertTrue(issues.size() >= 5); // 1 表 + 4 字段
    }

    @Test
    void commentMissingRule_goodExampleSql_noIssues() throws Exception {
        var parser = new SqlParserService();
        var rule = new CommentMissingRule();

        List<TableDef> tables = parser.parse(readExample("good-example.sql"));
        List<LintIssue> issues = rule.check(contextOf(tables.toArray(TableDef[]::new)));

        assertTrue(issues.isEmpty(), "good-example.sql 已包含 COMMENT ON 注释，不应误报缺注释");
    }

    @Test
    void forbiddenFieldRule_customParams() {
        var rule = new ForbiddenFieldNameRule();
        RuleContext ctx = RuleContext.builder()
                .tables(List.of(badTable()))
                .ruleParams(Map.of("forbiddenNames", List.of("userid")))
                .build();
        List<LintIssue> issues = rule.check(ctx);
        // 只检测自定义的禁用名
        assertTrue(issues.stream().anyMatch(i -> i.getColumnName().equalsIgnoreCase("userid")));
    }
}
