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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SQL 解析服务 —— 使用 JSqlParser 解析 PostgreSQL CREATE TABLE
 */
@Slf4j
@Service
public class SqlParserService {

    private static final Pattern TABLE_COMMENT_PATTERN = Pattern.compile(
            "(?is)COMMENT\\s+ON\\s+TABLE\\s+([^\\s]+)\\s+IS\\s+'((?:''|[^'])*)'\\s*;?");
    private static final Pattern COLUMN_COMMENT_PATTERN = Pattern.compile(
            "(?is)COMMENT\\s+ON\\s+COLUMN\\s+([^\\s]+)\\s+IS\\s+'((?:''|[^'])*)'\\s*;?");
    private static final Pattern MYSQL_TABLE_COMMENT_PATTERN = Pattern.compile(
            "(?is)CREATE\\s+TABLE\\s+(?:IF\\s+NOT\\s+EXISTS\\s+)?([^\\s(]+)\\s*\\(.*?\\)\\s*[^;]*?COMMENT\\s*=\\s*'((?:''|[^'])*)'");

    /**
     * 解析 SQL 文本，提取表定义
     */
    public List<TableDef> parse(String sql) {
        if (sql == null || sql.isBlank()) {
            return List.of();
        }

        List<TableDef> tables = new ArrayList<>();
        try {
            CommentIndex commentIndex = extractComments(sql);
            Statements statements = CCJSqlParserUtil.parseStatements(sql);
            for (Statement stmt : statements.getStatements()) {
                if (stmt instanceof CreateTable createTable) {
                    TableDef table = parseCreateTable(createTable);
                    applyComments(table, commentIndex);
                    tables.add(table);
                }
            }
        } catch (Exception e) {
            log.warn("SQL 解析失败: {}", e.getMessage());
            throw new IllegalArgumentException("SQL 解析失败: " + e.getMessage(), e);
        }
        return tables;
    }

    private CommentIndex extractComments(String sql) {
        Map<String, String> tableComments = new HashMap<>();
        Map<String, String> columnComments = new HashMap<>();

        Matcher tableMatcher = TABLE_COMMENT_PATTERN.matcher(sql);
        while (tableMatcher.find()) {
            String tableName = normalizeTableName(tableMatcher.group(1));
            tableComments.put(tableName, unescapeSqlString(tableMatcher.group(2)));
        }

        Matcher columnMatcher = COLUMN_COMMENT_PATTERN.matcher(sql);
        while (columnMatcher.find()) {
            List<String> parts = splitQualifiedName(columnMatcher.group(1));
            if (parts.size() >= 2) {
                String columnName = parts.get(parts.size() - 1);
                String tableName = parts.get(parts.size() - 2);
                columnComments.put(columnKey(tableName, columnName), unescapeSqlString(columnMatcher.group(2)));
            }
        }

        Matcher mysqlTableMatcher = MYSQL_TABLE_COMMENT_PATTERN.matcher(sql);
        while (mysqlTableMatcher.find()) {
            String tableName = normalizeTableName(mysqlTableMatcher.group(1));
            tableComments.put(tableName, unescapeSqlString(mysqlTableMatcher.group(2)));
        }

        return new CommentIndex(tableComments, columnComments);
    }

    private void applyComments(TableDef table, CommentIndex commentIndex) {
        String tableName = normalizeTableName(table.getName());
        table.setComment(commentIndex.tableComments().get(tableName));

        for (ColumnDef column : table.getColumns()) {
            String comment = commentIndex.columnComments().get(columnKey(tableName, column.getName()));
            if (comment != null) {
                column.setComment(comment);
            }
        }
    }

    private TableDef parseCreateTable(CreateTable createTable) {
        String tableName = stripIdentifierQuotes(createTable.getTable().getName());

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
        String name = stripIdentifierQuotes(colDef.getColumnName());
        String dataType = colDef.getColDataType().getDataType().trim();

        // 解析列参数（如 varchar(100)、numeric(10,2)）
        if (colDef.getColDataType().getArgumentsStringList() != null) {
            String args = String.join(", ", colDef.getColDataType().getArgumentsStringList());
            dataType = dataType + "(" + args + ")";
        }
        dataType = dataType.replaceAll("\\s+\\(", "(");

        // 检查 NOT NULL、PRIMARY KEY、DEFAULT 值
        boolean nullable = true;
        String defaultValue = null;
        String comment = null;
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
                if ("COMMENT".equals(spec) && i + 1 < specs.size()) {
                    comment = stripSqlString(specs.get(i + 1));
                }
            }
        }

        return ColumnDef.builder()
                .name(name)
                .dataType(dataType)
                .nullable(nullable)
                .defaultValue(defaultValue)
                .comment(comment)
                .build();
    }

    private String columnKey(String tableName, String columnName) {
        return normalizeIdentifier(tableName) + "." + normalizeIdentifier(columnName);
    }

    private String normalizeTableName(String name) {
        List<String> parts = splitQualifiedName(name);
        if (parts.isEmpty()) {
            return "";
        }
        return normalizeIdentifier(parts.get(parts.size() - 1));
    }

    private String stripIdentifierQuotes(String name) {
        if (name == null) {
            return null;
        }
        String normalized = name.trim();
        while ((normalized.startsWith("\"") && normalized.endsWith("\""))
                || (normalized.startsWith("`") && normalized.endsWith("`"))
                || (normalized.startsWith("[") && normalized.endsWith("]"))) {
            normalized = normalized.substring(1, normalized.length() - 1).trim();
        }
        return normalized;
    }

    private List<String> splitQualifiedName(String name) {
        if (name == null || name.isBlank()) {
            return List.of();
        }
        return List.of(name.split("\\.")).stream()
                .map(this::normalizeIdentifier)
                .filter(part -> !part.isBlank())
                .toList();
    }

    private String normalizeIdentifier(String name) {
        if (name == null) {
            return "";
        }
        String normalized = name.trim();
        while ((normalized.startsWith("\"") && normalized.endsWith("\""))
                || (normalized.startsWith("`") && normalized.endsWith("`"))
                || (normalized.startsWith("[") && normalized.endsWith("]"))) {
            normalized = normalized.substring(1, normalized.length() - 1).trim();
        }
        return normalized.toLowerCase();
    }

    private String unescapeSqlString(String value) {
        return value == null ? null : value.replace("''", "'");
    }

    private String stripSqlString(String value) {
        if (value == null) {
            return null;
        }
        String stripped = value.trim();
        if (stripped.length() >= 2 && stripped.startsWith("'") && stripped.endsWith("'")) {
            stripped = stripped.substring(1, stripped.length() - 1);
        }
        return unescapeSqlString(stripped);
    }

    private record CommentIndex(Map<String, String> tableComments, Map<String, String> columnComments) {
    }
}
