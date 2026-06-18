package com.dataspec.lint.engine;

import com.dataspec.lint.model.ColumnDef;
import com.dataspec.lint.model.LintIssue;
import com.dataspec.lint.model.LintResult;
import com.dataspec.lint.model.Severity;
import com.dataspec.lint.model.TableDef;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 修正 SQL 生成器单元测试(不依赖 Spring 容器)
 */
class FixedSqlGeneratorTest {

    private final FixedSqlGenerator generator = new FixedSqlGenerator();

    @Test
    void generate_emptyResult_returnsNull() {
        assertNull(generator.generate(null));
        assertNull(generator.generate(LintResult.of(List.of(), List.of())));
    }

    @Test
    void generate_cleanTable_producesEquivalentStructure() {
        // 规范表无 issue:fixedSql 应是等价的格式化 CREATE TABLE
        TableDef table = TableDef.builder()
                .name("user_order")
                .comment("用户订单表")
                .columns(List.of(
                        ColumnDef.builder().name("id").dataType("bigserial").nullable(false).build(),
                        ColumnDef.builder().name("user_id").dataType("bigint").nullable(false).build()
                ))
                .build();
        LintResult result = LintResult.of(List.of(table), List.of());

        String fixedSql = generator.generate(result);

        assertNotNull(fixedSql);
        assertTrue(fixedSql.contains("CREATE TABLE user_order"));
        assertTrue(fixedSql.contains("id bigserial NOT NULL"));
        assertTrue(fixedSql.contains("user_id bigint NOT NULL"));
        assertTrue(fixedSql.contains("COMMENT ON TABLE user_order IS '用户订单表'"));
    }

    @Test
    void generate_tableNameSnakeCase_renamesTable() {
        TableDef table = TableDef.builder()
                .name("UserOrder")
                .columns(List.of(
                        ColumnDef.builder().name("id").dataType("bigserial").nullable(false).build()
                ))
                .build();
        LintIssue issue = LintIssue.builder()
                .severity(Severity.ERROR)
                .ruleCode("table_naming_snake_case")
                .ruleName("表名 snake_case 校验")
                .tableName("UserOrder")
                .replacement("user_order")
                .before("UserOrder")
                .after("user_order")
                .confidence(90)
                .build();
        LintResult result = LintResult.of(List.of(table), List.of(issue));

        String fixedSql = generator.generate(result);

        assertNotNull(fixedSql);
        assertTrue(fixedSql.contains("CREATE TABLE user_order"));
        assertFalse(fixedSql.contains("UserOrder"));
    }

    @Test
    void generate_forbiddenAndRecommendedFieldNames_appliesReplacement() {
        // 验收场景:phone/create_time/update_time → mobile_no/created_at/updated_at
        // 注:phone 不在默认 recommended map,这里用 create_time/update_time 覆盖验收语义
        TableDef table = TableDef.builder()
                .name("t_user")
                .columns(List.of(
                        ColumnDef.builder().name("id").dataType("bigserial").nullable(false).build(),
                        ColumnDef.builder().name("create_time").dataType("timestamp").nullable(false).build(),
                        ColumnDef.builder().name("update_time").dataType("timestamp").nullable(false).build()
                ))
                .build();
        List<LintIssue> issues = List.of(
                LintIssue.builder()
                        .severity(Severity.SUGGESTION)
                        .ruleCode("recommended_field_name")
                        .ruleName("推荐字段名")
                        .tableName("t_user")
                        .columnName("create_time")
                        .replacement("created_at")
                        .before("create_time")
                        .after("created_at")
                        .confidence(95)
                        .build(),
                LintIssue.builder()
                        .severity(Severity.SUGGESTION)
                        .ruleCode("recommended_field_name")
                        .ruleName("推荐字段名")
                        .tableName("t_user")
                        .columnName("update_time")
                        .replacement("updated_at")
                        .before("update_time")
                        .after("updated_at")
                        .confidence(95)
                        .build()
        );
        LintResult result = LintResult.of(List.of(table), issues);

        String fixedSql = generator.generate(result);

        assertNotNull(fixedSql);
        assertTrue(fixedSql.contains("created_at"), "create_time 应被替换为 created_at");
        assertTrue(fixedSql.contains("updated_at"), "update_time 应被替换为 updated_at");
        assertFalse(fixedSql.contains("create_time"), "不应残留 create_time");
        assertFalse(fixedSql.contains("update_time"), "不应残留 update_time");
    }

    @Test
    void generate_missingRequiredColumns_appendsThem() {
        // 缺 created_at/updated_at/is_deleted:fixedSql 应补齐必备列
        TableDef table = TableDef.builder()
                .name("orders")
                .columns(List.of(
                        ColumnDef.builder().name("id").dataType("bigserial").nullable(false).build(),
                        ColumnDef.builder().name("amount_cent").dataType("bigint").nullable(false).build()
                ))
                .build();
        List<LintIssue> issues = List.of(
                LintIssue.builder()
                        .severity(Severity.ERROR)
                        .ruleCode("required_columns")
                        .ruleName("必含列校验")
                        .tableName("orders")
                        .replacement("created_at")
                        .after("created_at timestamp with time zone NOT NULL DEFAULT now()")
                        .confidence(85)
                        .build(),
                LintIssue.builder()
                        .severity(Severity.ERROR)
                        .ruleCode("required_columns")
                        .ruleName("必含列校验")
                        .tableName("orders")
                        .replacement("updated_at")
                        .after("updated_at timestamp with time zone NOT NULL DEFAULT now()")
                        .confidence(85)
                        .build(),
                LintIssue.builder()
                        .severity(Severity.ERROR)
                        .ruleCode("required_columns")
                        .ruleName("必含列校验")
                        .tableName("orders")
                        .replacement("is_deleted")
                        .after("is_deleted boolean NOT NULL DEFAULT false")
                        .confidence(85)
                        .build()
        );
        LintResult result = LintResult.of(List.of(table), issues);

        String fixedSql = generator.generate(result);

        assertNotNull(fixedSql);
        assertTrue(fixedSql.contains("created_at timestamp with time zone NOT NULL DEFAULT now()"),
                "应补齐 created_at 并保留类型、非空和默认值");
        assertTrue(fixedSql.contains("updated_at timestamp with time zone NOT NULL DEFAULT now()"),
                "应补齐 updated_at 并保留类型、非空和默认值");
        assertTrue(fixedSql.contains("is_deleted boolean NOT NULL DEFAULT false"),
                "应补齐 is_deleted 并保留类型、非空和默认值");
    }

    @Test
    void generate_conflictingFieldRenames_picksHighestConfidence() {
        // 同一字段被两条规则给出不同 replacement,取 confidence 高的
        TableDef table = TableDef.builder()
                .name("t1")
                .columns(List.of(
                        ColumnDef.builder().name("uid").dataType("bigint").nullable(false).build()
                ))
                .build();
        List<LintIssue> issues = List.of(
                LintIssue.builder()
                        .severity(Severity.ERROR)
                        .ruleCode("forbidden_field_name")
                        .tableName("t1")
                        .columnName("uid")
                        .replacement("user_id")
                        .confidence(90)
                        .build(),
                LintIssue.builder()
                        .severity(Severity.SUGGESTION)
                        .ruleCode("recommended_field_name")
                        .tableName("t1")
                        .columnName("uid")
                        .replacement("account_id")
                        .confidence(70)
                        .build()
        );
        LintResult result = LintResult.of(List.of(table), issues);

        String fixedSql = generator.generate(result);

        assertNotNull(fixedSql);
        assertTrue(fixedSql.contains("user_id"), "应取 confidence 高的 user_id");
        assertFalse(fixedSql.contains("account_id"));
    }

    @Test
    void generate_renameCollidesWithExistingColumn_abortsTable() {
        // 两个字段重命名后冲突(都建议改成 created_at):应放弃重建该表,整体返回 null
        TableDef table = TableDef.builder()
                .name("t1")
                .columns(List.of(
                        ColumnDef.builder().name("create_time").dataType("timestamp").build(),
                        ColumnDef.builder().name("ctime").dataType("timestamp").build()
                ))
                .build();
        List<LintIssue> issues = List.of(
                LintIssue.builder()
                        .ruleCode("recommended_field_name")
                        .tableName("t1")
                        .columnName("create_time")
                        .replacement("created_at")
                        .confidence(95)
                        .build(),
                LintIssue.builder()
                        .ruleCode("forbidden_field_name")
                        .tableName("t1")
                        .columnName("ctime")
                        .replacement("created_at")
                        .confidence(90)
                        .build()
        );
        LintResult result = LintResult.of(List.of(table), issues);

        assertNull(generator.generate(result), "重命名冲突时应返回 null 而非生成错误 SQL");
    }

    @Test
    void generate_multipleTables_rendersAll() {
        TableDef t1 = TableDef.builder()
                .name("users")
                .columns(List.of(
                        ColumnDef.builder().name("id").dataType("bigserial").nullable(false).build()
                ))
                .build();
        TableDef t2 = TableDef.builder()
                .name("UserOrder")
                .columns(List.of(
                        ColumnDef.builder().name("id").dataType("bigserial").nullable(false).build()
                ))
                .build();
        LintIssue renameIssue = LintIssue.builder()
                .ruleCode("table_naming_snake_case")
                .tableName("UserOrder")
                .replacement("user_order")
                .confidence(90)
                .build();
        LintResult result = LintResult.of(List.of(t1, t2), List.of(renameIssue));

        String fixedSql = generator.generate(result);

        assertNotNull(fixedSql);
        long createCount = fixedSql.lines().filter(l -> l.startsWith("CREATE TABLE")).count();
        assertEquals(2, createCount, "应渲染两张表");
    }
}
