package com.dataspec.lint.engine;

import com.dataspec.lint.model.ColumnDef;
import com.dataspec.lint.model.TableDef;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.Statements;
import net.sf.jsqlparser.statement.create.table.ColumnDefinition;
import net.sf.jsqlparser.statement.create.table.CreateTable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * SQL 解析服务 —— 使用 JSqlParser 解析 PostgreSQL CREATE TABLE
 */
@Slf4j
@Service
public class SqlParserService {

    /**
     * 解析 SQL 文本，提取表定义
     */
    public List<TableDef> parse(String sql) {
        List<TableDef> tables = new ArrayList<>();
        try {
            Statements statements = CCJSqlParserUtil.parseStatements(sql);
            for (Statement stmt : statements.getStatements()) {
                if (stmt instanceof CreateTable createTable) {
                    tables.add(parseCreateTable(createTable));
                }
            }
        } catch (Exception e) {
            log.warn("SQL 解析失败: {}", e.getMessage());
            throw new IllegalArgumentException("SQL 解析失败: " + e.getMessage(), e);
        }
        return tables;
    }

    private TableDef parseCreateTable(CreateTable createTable) {
        String tableName = createTable.getTable().getName();

        // 提取表注释
        String tableComment = null;
        if (createTable.getTableOptionsStrings() != null) {
            // JSqlParser 对 COMMENT 的支持有限，先留空
        }

        List<ColumnDef> columns = new ArrayList<>();
        if (createTable.getColumnDefinitions() != null) {
            for (ColumnDefinition colDef : createTable.getColumnDefinitions()) {
                columns.add(parseColumn(colDef));
            }
        }

        return TableDef.builder()
                .name(tableName)
                .comment(tableComment)
                .columns(columns)
                .build();
    }

    private ColumnDef parseColumn(ColumnDefinition colDef) {
        String name = colDef.getColumnName();
        String dataType = colDef.getColDataType().getDataType();

        // 解析列参数（如 varchar(100)、numeric(10,2)）
        if (colDef.getColDataType().getArgumentsStringList() != null) {
            String args = String.join(", ", colDef.getColDataType().getArgumentsStringList());
            dataType = dataType + "(" + args + ")";
        }

        // 检查 NOT NULL、PRIMARY KEY、DEFAULT 值
        boolean nullable = true;
        String defaultValue = null;
        if (colDef.getColumnSpecs() != null) {
            List<String> specs = colDef.getColumnSpecs();
            for (int i = 0; i < specs.size(); i++) {
                String spec = specs.get(i).toUpperCase();
                if ("NOT".equals(spec) && i + 1 < specs.size() && "NULL".equals(specs.get(i + 1).toUpperCase())) {
                    nullable = false;
                }
                // PRIMARY KEY 隐含 NOT NULL
                if ("PRIMARY".equals(spec) && i + 1 < specs.size() && "KEY".equals(specs.get(i + 1).toUpperCase())) {
                    nullable = false;
                }
                if ("DEFAULT".equals(spec) && i + 1 < specs.size()) {
                    defaultValue = specs.get(i + 1);
                }
            }
        }

        // 提取 COMMENT（PostgreSQL 使用 COMMENT ON COLUMN，不在 CREATE TABLE 内部）
        String comment = null;

        return ColumnDef.builder()
                .name(name)
                .dataType(dataType)
                .nullable(nullable)
                .defaultValue(defaultValue)
                .comment(comment)
                .build();
    }
}
