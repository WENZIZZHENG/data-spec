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
    void parseMySqlCreateTable_fillsInlineCommentsAndTypes() {
        String sql = """
                CREATE TABLE `t_user` (
                    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
                    `mobile_no` varchar(20) NOT NULL COMMENT '手机号',
                    `is_deleted` tinyint(1) NOT NULL DEFAULT 0 COMMENT '删除标记',
                    `created_at` datetime NOT NULL COMMENT '创建时间',
                    PRIMARY KEY (`id`)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';
                """;

        List<TableDef> tables = parser.parse(sql);

        assertEquals(1, tables.size());
        TableDef table = tables.get(0);
        assertEquals("t_user", table.getName());
        assertEquals("用户表", table.getComment());
        assertEquals(4, table.getColumns().size());

        var mobile = table.getColumns().stream()
                .filter(c -> c.getName().equals("mobile_no"))
                .findFirst()
                .orElseThrow();
        assertEquals("varchar(20)", mobile.getDataType().toLowerCase());
        assertEquals("手机号", mobile.getComment());
        assertFalse(mobile.isNullable());

        var deleted = table.getColumns().stream()
                .filter(c -> c.getName().equals("is_deleted"))
                .findFirst()
                .orElseThrow();
        assertEquals("tinyint(1)", deleted.getDataType().toLowerCase());
        assertEquals("0", deleted.getDefaultValue());
        assertEquals("删除标记", deleted.getComment());
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
