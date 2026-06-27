package com.dataspec.reverseimport.service.impl;

import com.dataspec.common.exception.BizException;
import com.dataspec.coverage.model.FieldCoverageReport;
import com.dataspec.coverage.service.FieldCoverageService;
import com.dataspec.dialect.service.SqlDialectCompatibilityService;
import com.dataspec.lint.model.ColumnDef;
import com.dataspec.lint.model.TableDef;
import com.dataspec.reverseimport.model.DatabaseConnectionReq;
import com.dataspec.reverseimport.model.DatabaseConnectionResult;
import com.dataspec.reverseimport.model.DatabaseTableInfo;
import com.dataspec.reverseimport.model.ReverseImportCompareResult;
import com.dataspec.reverseimport.model.ReverseImportPreview;
import com.dataspec.reverseimport.service.DatabaseReverseImportService;
import com.dataspec.reverseimport.service.ReverseImportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;

/**
 * 基于 JDBC metadata 的数据库直连反向导入。
 */
@Service
public class DatabaseReverseImportServiceImpl implements DatabaseReverseImportService {

    private static final String TYPE_POSTGRESQL = "postgresql";
    private static final String TYPE_MYSQL = "mysql";

    private final ReverseImportService reverseImportService;
    private final FieldCoverageService fieldCoverageService;
    private final ConnectionProvider connectionProvider;
    private final SqlDialectCompatibilityService dialectCompatibilityService = new SqlDialectCompatibilityService();

    @Autowired
    public DatabaseReverseImportServiceImpl(ReverseImportService reverseImportService,
                                            FieldCoverageService fieldCoverageService) {
        this(reverseImportService, fieldCoverageService, new DriverManagerConnectionProvider());
    }

    public DatabaseReverseImportServiceImpl(ReverseImportService reverseImportService,
                                            ConnectionProvider connectionProvider) {
        this(reverseImportService, null, connectionProvider);
    }

    public DatabaseReverseImportServiceImpl(ReverseImportService reverseImportService,
                                            FieldCoverageService fieldCoverageService,
                                            ConnectionProvider connectionProvider) {
        this.reverseImportService = reverseImportService;
        this.fieldCoverageService = fieldCoverageService;
        this.connectionProvider = connectionProvider;
    }

    @Override
    public DatabaseConnectionResult testConnection(DatabaseConnectionReq req) {
        try (Connection ignored = connectionProvider.open(req)) {
            return new DatabaseConnectionResult(true, "连接成功");
        } catch (SQLException | RuntimeException e) {
            return new DatabaseConnectionResult(false, "连接失败: " + e.getMessage());
        }
    }

    @Override
    public List<DatabaseTableInfo> listTables(DatabaseConnectionReq req) {
        validateConnectionReq(req);
        try (Connection connection = connectionProvider.open(req)) {
            return readTables(connection, req);
        } catch (SQLException e) {
            throw new BizException("读取数据库表失败: " + e.getMessage());
        }
    }

    @Override
    public ReverseImportPreview preview(DatabaseConnectionReq req) {
        validateConnectionReq(req);
        if (req.getTableNames() == null || req.getTableNames().isEmpty()) {
            throw new BizException("请至少选择一张表");
        }
        try (Connection connection = connectionProvider.open(req)) {
            List<TableDef> tables = readSelectedTables(connection, req);
            ReverseImportPreview preview = reverseImportService.previewTables(req.getProjectId(), tables);
            preview.setDialectDiagnostics(dialectCompatibilityService.diagnoseDatabase(req.getDatabaseType(), req.getSchemaName()));
            return preview;
        } catch (SQLException e) {
            throw new BizException("读取数据库表结构失败: " + e.getMessage());
        }
    }

    @Override
    public ReverseImportCompareResult compare(DatabaseConnectionReq req) {
        validateConnectionReq(req);
        if (req.getTableNames() == null || req.getTableNames().isEmpty()) {
            throw new BizException("请至少选择一张表");
        }
        try (Connection connection = connectionProvider.open(req)) {
            List<TableDef> tables = readSelectedTables(connection, req);
            return reverseImportService.compareTables(req.getProjectId(), tables);
        } catch (SQLException e) {
            throw new BizException("读取数据库表结构失败: " + e.getMessage());
        }
    }

    @Override
    public FieldCoverageReport coverage(DatabaseConnectionReq req) {
        validateConnectionReq(req);
        if (req.getTableNames() == null || req.getTableNames().isEmpty()) {
            throw new BizException("请至少选择一张表");
        }
        if (fieldCoverageService == null) {
            throw new BizException("字段覆盖率服务未初始化");
        }
        try (Connection connection = connectionProvider.open(req)) {
            List<TableDef> tables = readSelectedTables(connection, req);
            return fieldCoverageService.reportTables(req.getProjectId(), tables);
        } catch (SQLException e) {
            throw new BizException("读取数据库表结构失败: " + e.getMessage());
        }
    }

    private List<DatabaseTableInfo> readTables(Connection connection, DatabaseConnectionReq req) throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();
        List<DatabaseTableInfo> tables = new ArrayList<>();
        try (ResultSet rs = metaData.getTables(catalog(req), schemaPattern(req), "%", new String[]{"TABLE"})) {
            while (rs.next()) {
                tables.add(new DatabaseTableInfo(
                        rs.getString("TABLE_SCHEM"),
                        rs.getString("TABLE_NAME"),
                        rs.getString("TABLE_TYPE"),
                        rs.getString("REMARKS")));
            }
        }
        return tables;
    }

    private List<TableDef> readSelectedTables(Connection connection, DatabaseConnectionReq req) throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();
        Set<String> selectedNames = new LinkedHashSet<>(req.getTableNames());
        List<TableDef> tables = new ArrayList<>();
        for (DatabaseTableInfo tableInfo : readTables(connection, req)) {
            if (!containsIgnoreCase(selectedNames, tableInfo.tableName())) {
                continue;
            }
            tables.add(TableDef.builder()
                    .name(tableInfo.tableName())
                    .comment(tableInfo.comment())
                    .columns(readColumns(metaData, req, tableInfo.tableName()))
                    .build());
        }
        if (tables.isEmpty()) {
            throw new BizException("未找到所选表");
        }
        return tables;
    }

    private List<ColumnDef> readColumns(DatabaseMetaData metaData, DatabaseConnectionReq req, String tableName)
            throws SQLException {
        List<ColumnDef> columns = new ArrayList<>();
        try (ResultSet rs = metaData.getColumns(catalog(req), schemaPattern(req), tableName, "%")) {
            while (rs.next()) {
                columns.add(ColumnDef.builder()
                        .name(rs.getString("COLUMN_NAME"))
                        .dataType(formatDataType(rs))
                        .nullable(rs.getInt("NULLABLE") == DatabaseMetaData.columnNullable)
                        .defaultValue(rs.getString("COLUMN_DEF"))
                        .comment(rs.getString("REMARKS"))
                        .build());
            }
        }
        return columns;
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
            if (item != null && item.equalsIgnoreCase(value)) {
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

    private void validateConnectionReq(DatabaseConnectionReq req) {
        if (req == null || req.getProjectId() == null) {
            throw new BizException("项目ID不能为空");
        }
        if (isBlank(req.getDatabaseType()) || isBlank(req.getHost()) || isBlank(req.getDatabaseName())
                || isBlank(req.getUsername())) {
            throw new BizException("数据库连接信息不完整");
        }
        String type = databaseType(req);
        if (!TYPE_POSTGRESQL.equals(type) && !TYPE_MYSQL.equals(type)) {
            throw new BizException("暂不支持的数据库类型: " + req.getDatabaseType());
        }
    }

    private String databaseType(DatabaseConnectionReq req) {
        return req.getDatabaseType() == null ? "" : req.getDatabaseType().trim().toLowerCase(Locale.ROOT);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public interface ConnectionProvider {
        Connection open(DatabaseConnectionReq req) throws SQLException;
    }

    private static class DriverManagerConnectionProvider implements ConnectionProvider {

        @Override
        public Connection open(DatabaseConnectionReq req) throws SQLException {
            String type = req.getDatabaseType().trim().toLowerCase(Locale.ROOT);
            int port = req.getPort() != null ? req.getPort() : defaultPort(type);
            String url = switch (type) {
                case TYPE_POSTGRESQL -> "jdbc:postgresql://" + req.getHost() + ":" + port + "/" + req.getDatabaseName();
                case TYPE_MYSQL -> "jdbc:mysql://" + req.getHost() + ":" + port + "/" + req.getDatabaseName()
                        + "?useInformationSchema=true&remarks=true";
                default -> throw new SQLException("暂不支持的数据库类型: " + req.getDatabaseType());
            };
            Properties properties = new Properties();
            properties.setProperty("user", req.getUsername());
            properties.setProperty("password", req.getPassword() == null ? "" : req.getPassword());
            return DriverManager.getConnection(url, properties);
        }

        private int defaultPort(String type) throws SQLException {
            return switch (type) {
                case TYPE_POSTGRESQL -> 5432;
                case TYPE_MYSQL -> 3306;
                default -> throw new SQLException("暂不支持的数据库类型: " + type);
            };
        }
    }
}
