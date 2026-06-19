package com.dataspec.lint.engine;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 修正 SQL diff 生成测试。
 */
class SqlDiffGeneratorTest {

    private final SqlDiffGenerator generator = new SqlDiffGenerator();

    @Test
    void generate_returnsUnifiedDiffForChangedSql() {
        String diff = generator.generate("""
                CREATE TABLE UserOrder (
                    userId bigint
                );
                """, """
                CREATE TABLE user_order (
                    user_id bigint
                );
                """);

        assertTrue(diff.startsWith("--- original.sql\n+++ fixed.sql\n@@"));
        assertTrue(diff.contains("-CREATE TABLE UserOrder ("));
        assertTrue(diff.contains("+CREATE TABLE user_order ("));
        assertTrue(diff.contains("-    userId bigint"));
        assertTrue(diff.contains("+    user_id bigint"));
    }

    @Test
    void generate_returnsNullWhenSqlIsUnchangedOrMissing() {
        assertNull(generator.generate("CREATE TABLE users (id bigint);", "CREATE TABLE users (id bigint);"));
        assertNull(generator.generate("CREATE TABLE users (id bigint);", null));
        assertNull(generator.generate(null, "CREATE TABLE users (id bigint);"));
    }
}
