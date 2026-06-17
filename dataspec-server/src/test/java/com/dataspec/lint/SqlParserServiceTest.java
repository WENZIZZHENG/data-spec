package com.dataspec.lint;

import com.dataspec.lint.engine.SqlParserService;
import com.dataspec.lint.model.TableDef;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SQL 解析服务测试
 */
class SqlParserServiceTest {

    private final SqlParserService parser = new SqlParserService();

    private String readExample(String fileName) throws Exception {
        return Files.readString(Path.of("..", "examples", fileName));
    }

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
    void parseGoodExample_fillsCommentOnTableAndColumns() throws Exception {
        List<TableDef> tables = parser.parse(readExample("good-example.sql"));

        assertEquals(1, tables.size());
        TableDef table = tables.get(0);
        assertEquals("users", table.getName());
        assertEquals("用户表", table.getComment());
        assertEquals(9, table.getColumns().size());

        var username = table.getColumns().stream()
                .filter(c -> c.getName().equals("username"))
                .findFirst()
                .orElseThrow();
        assertEquals("用户名", username.getComment());
        assertEquals("varchar(50)", username.getDataType().toLowerCase());
        assertFalse(username.isNullable());

        var phone = table.getColumns().stream()
                .filter(c -> c.getName().equals("phone"))
                .findFirst()
                .orElseThrow();
        assertEquals("手机号", phone.getComment());
        assertTrue(phone.isNullable());
    }

    @Test
    void parseBlankSql_returnsEmptyTables() {
        assertTrue(parser.parse("   \n\t  ").isEmpty());
    }

    @Test
    void parseInvalidSql_throwsException() {
        String sql = "THIS IS NOT SQL AT ALL";
        assertThrows(IllegalArgumentException.class, () -> parser.parse(sql));
    }
}
