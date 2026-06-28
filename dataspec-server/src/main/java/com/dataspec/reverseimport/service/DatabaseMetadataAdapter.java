package com.dataspec.reverseimport.service;

import com.dataspec.lint.model.TableDef;
import com.dataspec.reverseimport.model.DatabaseConnectionReq;
import com.dataspec.reverseimport.model.DatabaseSchemaDump;
import com.dataspec.reverseimport.model.DatabaseTableInfo;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * 数据库 metadata 适配层：负责把不同 JDBC 方言规范化为 DataSpec schema dump。
 */
public interface DatabaseMetadataAdapter {

    List<DatabaseTableInfo> listTables(Connection connection, DatabaseConnectionReq req) throws SQLException;

    DatabaseSchemaDump exportDump(Connection connection, DatabaseConnectionReq req) throws SQLException;

    List<TableDef> toTableDefs(Long projectId, DatabaseSchemaDump dump);
}
