package com.dataspec.reverseimport.service.impl;

import com.dataspec.common.exception.BizException;
import com.dataspec.common.sanitize.SensitiveDataSanitizer;
import com.dataspec.lint.model.ColumnDef;
import com.dataspec.lint.model.TableDef;
import com.dataspec.reverseimport.model.DatabaseConnectionReq;
import com.dataspec.reverseimport.model.DatabaseSchemaColumn;
import com.dataspec.reverseimport.model.DatabaseSchemaDump;
import com.dataspec.reverseimport.model.DatabaseSchemaSource;
import com.dataspec.reverseimport.model.DatabaseSchemaTable;
import com.dataspec.reverseimport.model.DatabaseTableInfo;
import com.dataspec.reverseimport.service.DatabaseMetadataAdapter;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 基于 JDBC `DatabaseMetaData` 的 PostgreSQL/MySQL metadata 适配器。
 */
@Component
public class JdbcDatabaseMetadataAdapter implements DatabaseMetadataAdapter {

    private static final String TYPE_POSTGRESQL = "postgresql";
    private static final String TYPE_MYSQL = "mysql";

    @Override
    public List<DatabaseTableInfo> listTables(Connection connection, DatabaseConnectionReq req) throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();
        List<DatabaseTableInfo> tables = new ArrayList<>();
        try (ResultSet rs = metaData.getTables(catalog(req), schemaPattern(req), "%", new String[]{"TABLE"})) {
            while (rs.next()) {
                tables.add(new DatabaseTableInfo(
                        safeText(rs.getString("TABLE_SCHEM")),
                        safeText(rs.getString("TABLE_NAME")),
                        safeText(rs.getString("TABLE_TYPE")),
                        safeText(rs.getString("REMARKS"))));
            }
        }
        return tables;
    }

    @Override
    public DatabaseSchemaDump exportDump(Connection connection, DatabaseConnectionReq req) throws SQLException {
        if (req.getTableNames() == null || req.getTableNames().isEmpty()) {
            throw new BizException("请至少选择一张表");
        }
        DatabaseMetaData metaData = connection.getMetaData();
        List<DatabaseTableInfo> availableTables = listTables(connection, req);
        Set<String> selectedNames = new LinkedHashSet<>(req.getTableNames());

        DatabaseSchemaDump dump = new DatabaseSchemaDump();
        dump.setProjectId(req.getProjectId());
        dump.setDatabaseType(databaseType(req).toUpperCase(Locale.ROOT));
        dump.setDatabaseName(safeText(req.getDatabaseName()));
        dump.setSchemaName(safeText(schemaPattern(req)));
        dump.setGeneratedAt(Instant.now().toString());
        dump.setSource(buildSource(metaData, req));
        if (TYPE_MYSQL.equals(databaseType(req)) && req.getSchemaName() != null && !req.getSchemaName().isBlank()) {
            dump.getWarnings().add("MySQL metadata 使用 databaseName 作为 catalog，schemaName 不参与过滤。");
        }

        for (DatabaseTableInfo tableInfo : availableTables) {
            if (!containsIgnoreCase(selectedNames, tableInfo.tableName())) {
                continue;
            }
            dump.getTables().add(readTable(metaData, req, tableInfo));
        }
        if (dump.getTables().isEmpty()) {
            throw new BizException("未找到所选表");
        }
        dump.getSource().setTableCount(dump.getTables().size());
        return dump;
    }

    @Override
    public List<TableDef> toTableDefs(Long projectId, DatabaseSchemaDump dump) {
        validateDump(projectId, dump);
        return dump.getTables().stream()
                .map(table -> TableDef.builder()
                        .name(table.getTableName())
                        .comment(table.getComment())
                        .columns(table.getColumns().stream()
                                .map(column -> ColumnDef.builder()
                                        .name(column.getColumnName())
                                        .dataType(column.getDataType())
                                        .nullable(Boolean.TRUE.equals(column.getNullable()))
                                        .defaultValue(column.getDefaultValue())
                                        .comment(column.getComment())
                                        .build())
                                .toList())
                        .build())
                .toList();
    }

    private DatabaseSchemaSource buildSource(DatabaseMetaData metaData, DatabaseConnectionReq req) throws SQLException {
        DatabaseSchemaSource source = new DatabaseSchemaSource();
        source.setDatabaseProductName(safeText(metaData.getDatabaseProductName()));
        source.setDatabaseProductVersion(safeText(metaData.getDatabaseProductVersion()));
        source.setCatalogName(safeText(catalog(req)));
        source.setSchemaName(safeText(schemaPattern(req)));
        source.setSelectedTableNames(req.getTableNames().stream().map(this::safeText).toList());
        return source;
    }

    private DatabaseSchemaTable readTable(DatabaseMetaData metaData, DatabaseConnectionReq req, DatabaseTableInfo tableInfo)
            throws SQLException {
        DatabaseSchemaTable table = new DatabaseSchemaTable();
        table.setSchemaName(safeText(tableInfo.schemaName()));
        table.setTableName(safeText(tableInfo.tableName()));
        table.setTableType(safeText(tableInfo.tableType()));
        table.setComment(safeText(tableInfo.comment()));
        table.setColumns(readColumns(metaData, req, tableInfo));
        return table;
    }

    private List<DatabaseSchemaColumn> readColumns(DatabaseMetaData metaData, DatabaseConnectionReq req, DatabaseTableInfo tableInfo)
            throws SQLException {
        List<DatabaseSchemaColumn> columns = new ArrayList<>();
        String tableSchema = TYPE_MYSQL.equals(databaseType(req)) ? null : firstNonBlank(tableInfo.schemaName(), schemaPattern(req));
        try (ResultSet rs = metaData.getColumns(catalog(req), tableSchema, tableInfo.tableName(), "%")) {
            while (rs.next()) {
                DatabaseSchemaColumn column = new DatabaseSchemaColumn();
                column.setColumnName(safeText(rs.getString("COLUMN_NAME")));
                column.setDataType(formatDataType(rs));
                column.setNullable(rs.getInt("NULLABLE") == DatabaseMetaData.columnNullable);
                column.setDefaultValue(safeText(rs.getString("COLUMN_DEF")));
                column.setComment(safeText(rs.getString("REMARKS")));
                column.setOrdinalPosition(rs.getInt("ORDINAL_POSITION"));
                columns.add(column);
            }
        }
        return columns;
    }

    private void validateDump(Long projectId, DatabaseSchemaDump dump) {
        if (projectId == null) {
            throw new BizException("项目ID不能为空");
        }
        if (dump == null) {
            throw new BizException("schema dump 不能为空");
        }
        if (dump.getProjectId() != null && !projectId.equals(dump.getProjectId())) {
            throw new BizException("schema dump 项目ID不匹配");
        }
        if (dump.getTables() == null || dump.getTables().isEmpty()) {
            throw new BizException("schema dump 未包含表结构");
        }
        for (DatabaseSchemaTable table : dump.getTables()) {
            if (table == null || isBlank(table.getTableName())) {
                throw new BizException("schema dump 存在缺少表名的表结构");
            }
            if (table.getColumns() == null || table.getColumns().isEmpty()) {
                throw new BizException("schema dump 表缺少列结构: " + table.getTableName());
            }
            for (DatabaseSchemaColumn column : table.getColumns()) {
                if (column == null || isBlank(column.getColumnName())) {
                    throw new BizException("schema dump 表存在缺少列名的列结构: " + table.getTableName());
                }
            }
        }
    }

    private String formatDataType(ResultSet rs) throws SQLException {
        String typeName = rs.getString("TYPE_NAME");
        int size = rs.getInt("COLUMN_SIZE");
        int scale = rs.getInt("DECIMAL_DIGITS");
        if (typeName == null || typeName.isBlank()) {
            return "";
        }
        String normalized = typeName.toUpperCase(Locale.ROOT);
        if (size > 0 && isSizedType(normalized)) {
            if (scale > 0 && isDecimalType(normalized)) {
                return normalized + "(" + size + "," + scale + ")";
            }
            return normalized + "(" + size + ")";
        }
        return normalized;
    }

    private boolean isSizedType(String typeName) {
        return typeName.contains("CHAR")
                || typeName.contains("TEXT")
                || typeName.contains("NUMERIC")
                || typeName.contains("DECIMAL")
                || typeName.contains("NUMBER");
    }

    private boolean isDecimalType(String typeName) {
        return typeName.contains("NUMERIC") || typeName.contains("DECIMAL") || typeName.contains("NUMBER");
    }

    private boolean containsIgnoreCase(Set<String> values, String value) {
        for (String item : values) {
            if (item != null && value != null && item.equalsIgnoreCase(value)) {
                return true;
            }
        }
        return false;
    }

    private String catalog(DatabaseConnectionReq req) {
        return TYPE_MYSQL.equals(databaseType(req)) ? req.getDatabaseName() : null;
    }

    private String schemaPattern(DatabaseConnectionReq req) {
        if (TYPE_MYSQL.equals(databaseType(req))) {
            return null;
        }
        if (req.getSchemaName() == null || req.getSchemaName().isBlank()) {
            return "public";
        }
        return req.getSchemaName();
    }

    private String databaseType(DatabaseConnectionReq req) {
        return req.getDatabaseType() == null ? "" : req.getDatabaseType().trim().toLowerCase(Locale.ROOT);
    }

    private String safeText(String value) {
        if (value == null) {
            return null;
        }
        return SensitiveDataSanitizer.redactText(value, 1000);
    }

    private String firstNonBlank(String first, String second) {
        return isBlank(first) ? second : first;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
