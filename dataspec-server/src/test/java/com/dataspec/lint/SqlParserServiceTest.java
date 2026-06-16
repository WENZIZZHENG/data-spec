package com.dataspec.lint;

import com.dataspec.lint.engine.SqlParserService;
import com.dataspec.lint.model.TableDef;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SQL 解析服务测试
 */
class SqlParserServiceTest {

    private final SqlParserService parser = new SqlParserService();

    @Test
    void parsePostgreSqlCreateTable() {
        String sql = """
                CREATE TABLE user_order (
                    id bigserial PRIMARY KEY,
                    user_id bigint NOT NULL,
                    order_no varchar(64) NOT NULL,
                    amount_cent bigint NOT NULL DEFAULT 0,
                    status integer NOT NULL DEFAULT 0,
                    created_at timestamp with time zone NOT NULL DEFAULT now(),
                    updated_at timestamp with time zone NOT NULL DEFAULT now(),
                    is_deleted boolean NOT NULL DEFAULT false
                );
                """;

        List<TableDef> tables = parser.parse(sql);
        assertEquals(1, tables.size());

        TableDef table = tables.get(0);
        assertEquals("user_order", table.getName());
        assertEquals(8, table.getColumns().size());

        // 验证 id 字段
        var idCol = table.getColumns().get(0);
        assertEquals("id", idCol.getName());
        assertEquals("bigserial", idCol.getDataType().toLowerCase());
        assertFalse(idCol.isNullable());

        // 验证 nullable 字段
        var userIdCol = table.getColumns().get(1);
        assertEquals("user_id", userIdCol.getName());
        assertFalse(userIdCol.isNullable());
    }

    @Test
    void parseMultipleTables() {
        String sql = """
                CREATE TABLE users (
                    id bigserial PRIMARY KEY,
                    username varchar(50) NOT NULL
                );
                CREATE TABLE orders (
                    id bigserial PRIMARY KEY,
                    user_id bigint NOT NULL
                );
                """;

        List<TableDef> tables = parser.parse(sql);
        assertEquals(2, tables.size());
    }

    @Test
    void parseInvalidSql_throwsException() {
        String sql = "THIS IS NOT SQL AT ALL";
        assertThrows(IllegalArgumentException.class, () -> parser.parse(sql));
    }
}
